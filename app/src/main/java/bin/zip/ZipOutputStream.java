/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package bin.zip;

import bin.zip.encoding.ZipEncoding;
import bin.zip.encoding.ZipEncodingHelper;
import bin.zip.extrafield.UnicodeCommentExtraField;
import bin.zip.extrafield.UnicodePathExtraField;
import gm.tieba.tabswitch.util.RepackageProcessor;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.ZipException;

import static bin.zip.ZipLong.putLong;
import static bin.zip.ZipShort.putShort;
import static bin.zip.encoding.ZipEncodingHelper.UTF8_ZIP_ENCODING;

/**
 * Reimplementation of {@link java.util.zip.ZipOutputStream
 * java.util.zip.ZipOutputStream} that does handle the extended
 * functionality of this package, especially internal/external file
 * attributes and extra fields with different layouts for local file
 * data and central directory entries.
 * <p>
 * <p>This class will try to use {@link RandomAccessFile
 * RandomAccessFile} when you know that the output is going to go to a
 * file.</p>
 * <p>
 * <p>If RandomAccessFile cannot be used, this implementation will use
 * a Data Descriptor to store size and CRC information for {@link
 * #DEFLATED DEFLATED} entries, this means, you don't need to
 * calculate them yourself.  Unfortunately this is not possible for
 * the {@link #STORED STORED} method, here setting the CRC and
 * uncompressed size information is required before {@link
 * #putNextEntry putNextEntry} can be called.</p>
 */
public class ZipOutputStream extends FilterOutputStream {
    public static final int LEVEL_BEST = Deflater.BEST_COMPRESSION;
    public static final int LEVEL_BETTER = 7;
    public static final int LEVEL_DEFAULT = Deflater.DEFAULT_COMPRESSION;
    public static final int LEVEL_FASTER = 3;
    public static final int LEVEL_FASTEST = Deflater.BEST_SPEED;

    private static final int BYTE_MASK = 0xFF;
    private static final int SHORT = 2;
    private static final int WORD = 4;
    public static final int BUFFER_SIZE = 10240;
    /* 
     * Apparently Deflater.setInput gets slowed down a lot on Sun JVMs
     * when it gets handed a really big buffer.  See
     * https://issues.apache.org/bugzilla/show_bug.cgi?id=45396
     *
     * Using a buffer size of 8 kB proved to be a good compromise
     */
    private static final int DEFLATER_BLOCK_SIZE = 8192;

    /**
     * Compression method for deflated entries.
     *
     * @since 1.1
     */
    public static final int DEFLATED = java.util.zip.ZipEntry.DEFLATED;

    /**
     * Default compression level for deflated entries.
     *
     * @since Ant 1.7
     */
    public static final int DEFAULT_COMPRESSION = Deflater.DEFAULT_COMPRESSION;

    /**
     * Compression method for stored entries.
     *
     * @since 1.1
     */
    public static final int STORED = java.util.zip.ZipEntry.STORED;

    /**
     * default encoding for file names and comment.
     */
    static final String DEFAULT_ENCODING = null;

    /**
     * General purpose flag, which indicates that filenames are
     * written in utf-8.
     */
    public static final int UFT8_NAMES_FLAG = 1 << 11;

    /**
     * General purpose flag, which indicates that filenames are
     * written in utf-8.
     *
     * @deprecated use {@link #UFT8_NAMES_FLAG} instead
     */
    public static final int EFS_FLAG = UFT8_NAMES_FLAG;

    /**
     * Current entry.
     *
     * @since 1.1
     */
    private ZipEntry entry;

    /**
     * The file comment.
     *
     * @since 1.1
     */
    private String comment = "";

    /**
     * Compression level for next entry.
     *
     * @since 1.1
     */
    private int level = DEFAULT_COMPRESSION;

    /**
     * Has the compression level changed when compared to the last
     * entry?
     *
     * @since 1.5
     */
    private boolean hasCompressionLevelChanged = false;

    /**
     * Default compression method for next entry.
     *
     * @since 1.1
     */
    private int method = java.util.zip.ZipEntry.DEFLATED;

    /**
     * List of ZipEntries written so far.
     *
     * @since 1.1
     */
    private final List<ZipEntry> entries = new LinkedList<>();

    /**
     * CRC instance to avoid parsing DEFLATED data twice.
     *
     * @since 1.1
     */
    private final CRC32 crc = new CRC32();

    /**
     * Count the bytes written to out.
     *
     * @since 1.1
     */
    private long written = 0;

    /**
     * Data for local header data
     *
     * @since 1.1
     */
    private long dataStart = 0;

    /**
     * Offset for CRC entry in the local file header data for the
     * current entry starts here.
     *
     * @since 1.15
     */
    private long localDataStart = 0;

    /**
     * Start of central directory.
     *
     * @since 1.1
     */
    private long cdOffset = 0;

    /**
     * Length of central directory.
     *
     * @since 1.1
     */
    private long cdLength = 0;

    /**
     * Helper, a 0 as ZipShort.
     *
     * @since 1.1
     */
    private static final byte[] ZERO = {0, 0};

    /**
     * Helper, a 0 as ZipLong.
     *
     * @since 1.1
     */
    private static final byte[] LZERO = {0, 0, 0, 0};

    /**
     * Holds the offsets of the LFH starts for each entry.
     *
     * @since 1.1
     */
    private final Map<ZipEntry, Long> offsets = new HashMap<>();

    /**
     * The encoding to use for filenames and the file comment.
     * <p>
     * <p>For a list of possible values see <a
     * href="http://java.sun.com/j2se/1.5.0/docs/guide/intl/encoding.doc.html">http://java.sun.com/j2se/1.5.0/docs/guide/intl/encoding.doc.html</a>.
     * Defaults to the platform's default character encoding.</p>
     *
     * @since 1.3
     */
    private String encoding = null;

    /**
     * The zip encoding to use for filenames and the file comment.
     * <p>
     * This field is of internal use and will be set in {@link
     * #setEncoding(String)}.
     */
    private ZipEncoding zipEncoding = UTF8_ZIP_ENCODING;

    // CheckStyle:VisibilityModifier OFF - bc

    /**
     * This Deflater object is used for output.
     * <p>
     * <p>This attribute is only protected to provide a level of API
     * backwards compatibility.  This class used to extend {@link
     * java.util.zip.DeflaterOutputStream DeflaterOutputStream} up to
     * Revision 1.13.</p>
     *
     * @since 1.14
     */
    protected Deflater def = new Deflater(level, true);

    /**
     * This buffer servers as a Deflater.
     * <p>
     * <p>This attribute is only protected to provide a level of API
     * backwards compatibility.  This class used to extend {@link
     * java.util.zip.DeflaterOutputStream DeflaterOutputStream} up to
     * Revision 1.13.</p>
     *
     * @since 1.14
     */
    protected byte[] buf = new byte[512];

    // CheckStyle:VisibilityModifier ON

    /**
     * Optional random access output.
     *
     * @since 1.14
     */
    private RandomAccessFile raf = null;

    /**
     * whether to use the general purpose bit flag when writing UTF-8
     * filenames or not.
     */
    private boolean useUTF8Flag = true;

    /**
     * Whether to encode non-encodable file names as UTF-8.
     */
    private boolean fallbackToUTF8 = false;

    /**
     * whether to create UnicodePathExtraField-s for each entry.
     */
    private UnicodeExtraFieldPolicy createUnicodeExtraFields =
            UnicodeExtraFieldPolicy.NEVER;

    /**
     * Creates a new ZIP OutputStream filtering the underlying stream.
     *
     * @param out the outputstream to zip
     * @since 1.1
     */
    public ZipOutputStream(OutputStream out) {
        super(out);
    }

    /**
     * Creates a new ZIP OutputStream writing to a File.  Will use
     * random access if possible.
     *
     * @param file the file to zip to
     * @throws IOException on error
     * @since 1.14
     */
    public ZipOutputStream(File file) throws IOException {
        //noinspection ConstantConditions
        super(null);

        try {
            raf = new RandomAccessFile(file, "rw");
            raf.setLength(0);
        } catch (IOException e) {
            if (raf != null) {
                try {
                    raf.close();
                } catch (IOException inner) {
                    // ignore
                }
                raf = null;
            }
            out = new FileOutputStream(file);
        }
    }

    /**
     * This method indicates whether this archive is writing to a
     * seekable stream (i.e., to a random access file).
     * <p>
     * <p>For seekable streams, you don't need to calculate the CRC or
     * uncompressed size for {@link #STORED} entries before
     * invoking {@link #putNextEntry}.
     *
     * @return true if seekable
     * @since 1.17
     */
    public boolean isSeekable() {
        return raf != null;
    }

    /**
     * The encoding to use for filenames and the file comment.
     * <p>
     * <p>For a list of possible values see <a
     * href="http://java.sun.com/j2se/1.5.0/docs/guide/intl/encoding.doc.html">http://java.sun.com/j2se/1.5.0/docs/guide/intl/encoding.doc.html</a>.
     * Defaults to the platform's default character encoding.</p>
     *
     * @param encoding the encoding value
     * @since 1.3
     */
    public void setEncoding(final String encoding) {
        this.encoding = encoding;
        boolean isUTF8 = ZipEncodingHelper.isUTF8(encoding);
        this.zipEncoding = isUTF8 ? UTF8_ZIP_ENCODING : ZipEncodingHelper.getZipEncoding(encoding);
        if (useUTF8Flag && !isUTF8) {
            useUTF8Flag = false;
        }
    }

    public void setZipEncoding(final ZipEncoding zipEncoding) {
        this.zipEncoding = zipEncoding;
        this.encoding = zipEncoding.getEncoding();
        if (useUTF8Flag && !ZipEncodingHelper.isUTF8(encoding)) {
            useUTF8Flag = false;
        }
    }


    /**
     * The encoding to use for filenames and the file comment.
     *
     * @return null if using the platform's default character encoding.
     * @since 1.3
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * Whether to set the language encoding flag if the file name
     * encoding is UTF-8.
     * <p>
     * <p>Defaults to true.</p>
     */
    public void setUseLanguageEncodingFlag(boolean b) {
        useUTF8Flag = b && ZipEncodingHelper.isUTF8(encoding);
    }

    /**
     * Whether to create Unicode Extra Fields.
     * <p>
     * <p>Defaults to NEVER.</p>
     */
    public void setCreateUnicodeExtraFields(UnicodeExtraFieldPolicy b) {
        createUnicodeExtraFields = b;
    }

    /**
     * Whether to fall back to UTF and the language encoding flag if
     * the file name cannot be encoded using the specified encoding.
     * <p>
     * <p>Defaults to false.</p>
     */
    public void setFallbackToUTF8(boolean b) {
        fallbackToUTF8 = b;
    }

    /**
     * Finishs writing the contents and closes this as well as the
     * underlying stream.
     *
     * @throws IOException on error
     * @since 1.1
     */
    public void finish() throws IOException {
        closeEntry();
        cdOffset = written;
        for (ZipEntry entry1 : entries) {
            writeCentralFileHeader(entry1);
        }
        cdLength = written - cdOffset;
        writeCentralDirectoryEnd();
        offsets.clear();
        entries.clear();
        def.end();
    }

    /**
     * Writes all necessary data for this entry.
     *
     * @throws IOException on error
     * @since 1.1
     */
    public void closeEntry() throws IOException {
        if (entry == null) {
            return;
        }

        if (currentIsRawEntry)
            crc.reset();
        else {
            long realCrc = crc.getValue();
            crc.reset();

            if (entry.getMethod() == DEFLATED) {
                def.finish();
                while (!def.finished()) {
                    deflate();
                }

                entry.setSize(adjustToLong(def.getTotalIn()));
                entry.setCompressedSize(adjustToLong(def.getTotalOut()));
                entry.setCrc(realCrc);

                def.reset();

                written += entry.getCompressedSize();
            } else if (raf == null) {
                if (entry.getCrc() != realCrc) {
                    throw new ZipException("bad CRC checksum for entry "
                            + entry.getName() + ": "
                            + Long.toHexString(entry.getCrc())
                            + " instead of "
                            + Long.toHexString(realCrc));
                }

                if (entry.getSize() != written - dataStart) {
                    throw new ZipException("bad size for entry "
                            + entry.getName() + ": "
                            + entry.getSize()
                            + " instead of "
                            + (written - dataStart));
                }
            } else { /* method is STORED and we used RandomAccessFile */
                long size = written - dataStart;

                entry.setSize(size);
                entry.setCompressedSize(size);
                entry.setCrc(realCrc);
            }

            // If random access output, write the local file header containing
            // the correct CRC and compressed/uncompressed sizes
            if (raf != null) {
                long save = raf.getFilePointer();

                raf.seek(localDataStart);
                byte[] data = new byte[12];
                putLong(entry.getCrc(), data, 0);
                putLong(entry.getCompressedSize(), data, 4);
                putLong(entry.getSize(), data, 8);
                writeOut(data);
                raf.seek(save);
            }
        }

        writeDataDescriptor(entry);
        entry = null;
    }

    public void writeFully(InputStream is) throws IOException {
        int len;
        byte[] b = new byte[BUFFER_SIZE];
        while ((len = is.read(b)) > 0) {
            write(b, 0, len);
            RepackageProcessor.writtenSize +=len;
        }
    }

    public void copyZipEntry(ZipEntry zipEntry, ZipFile zipFile) throws IOException {
        InputStream rawInputStream = zipFile.getRawInputStream(zipEntry);
        putNextRawEntry(zipEntry);
        int len;
        byte[] b = new byte[BUFFER_SIZE];
        while ((len = rawInputStream.read(b)) > 0) {
            writeRaw(b, 0, len);
            RepackageProcessor.writtenSize +=len;
        }
        closeEntry();
    }

    private boolean currentIsRawEntry;

    public void putNextEntry(String entryName) throws IOException {
        putNextEntry(new ZipEntry(entryName));
    }

    /**
     * Begin writing next entry.
     *
     * @param ze the entry to write
     * @throws IOException on error
     * @since 1.1
     */
    public void putNextEntry(ZipEntry ze) throws IOException {
        closeEntry();

        entry = ze;
        entries.add(entry);

        currentIsRawEntry = false;

        //noinspection WrongConstant
        if (entry.getMethod() == -1) { // not specified
            entry.setMethod(method);
        }

        if (entry.getTime() == -1) { // not specified
            entry.setTime(System.currentTimeMillis());
        }

        // Size/CRC not required if RandomAccessFile is used
        if (entry.getMethod() == STORED && raf == null) {
            if (entry.getSize() == -1) {
                throw new ZipException("uncompressed size is required for"
                        + " STORED method when not writing to a"
                        + " file");
            }
            if (entry.getCrc() == -1) {
                throw new ZipException("crc checksum is required for STORED"
                        + " method when not writing to a file");
            }
            entry.setCompressedSize(entry.getSize());
        }

        if (entry.getMethod() == DEFLATED && hasCompressionLevelChanged) {
            def.setLevel(level);
            hasCompressionLevelChanged = false;
        }
        writeLocalFileHeader(entry);
    }

    public void putNextRawEntry(ZipEntry ze) throws IOException {
        closeEntry();

        entry = ze;
        entries.add(entry);

        currentIsRawEntry = true;

        // Size/CRC not required if RandomAccessFile is used
        if (entry.getMethod() == STORED && raf == null) {
            if (entry.getSize() == -1) {
                throw new ZipException("uncompressed size is required for"
                        + " STORED method when not writing to a"
                        + " file");
            }
            if (entry.getCrc() == -1) {
                throw new ZipException("crc checksum is required for STORED"
                        + " method when not writing to a file");
            }
            entry.setCompressedSize(entry.getSize());
        }

        if (entry.getMethod() == DEFLATED && hasCompressionLevelChanged) {
            def.setLevel(level);
            hasCompressionLevelChanged = false;
        }
        writeLocalFileHeader(entry);
    }

    /**
     * Set the file comment.
     *
     * @param comment the comment
     * @since 1.1
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * Sets the compression level for subsequent entries.
     * <p>
     * <p>Default is Deflater.DEFAULT_COMPRESSION.</p>
     *
     * @param level the compression level.
     * @throws IllegalArgumentException if an invalid compression
     *                                  level is specified.
     * @since 1.1
     */
    public void setLevel(int level) {
        if (level < Deflater.DEFAULT_COMPRESSION
                || level > Deflater.BEST_COMPRESSION) {
            throw new IllegalArgumentException("Invalid compression level: "
                    + level);
        }
        hasCompressionLevelChanged = (this.level != level);
        this.level = level;
    }

    /**
     * Sets the default compression method for subsequent entries.
     * <p>
     * <p>Default is DEFLATED.</p>
     *
     * @param method an <code>int</code> from java.util.zip.ZipEntry
     * @since 1.1
     */
    public void setMethod(int method) {
        this.method = method;
    }

    /**
     * Writes bytes to ZIP entry.
     *
     * @param b      the byte array to write
     * @param offset the start position to write from
     * @param length the number of bytes to write
     * @throws IOException on error
     */
    public void write(byte[] b, int offset, int length) throws IOException {
        if (entry.getMethod() == DEFLATED) {
            if (length > 0) {
                if (!def.finished()) {
                    if (length <= DEFLATER_BLOCK_SIZE) {
                        def.setInput(b, offset, length);
                        deflateUntilInputIsNeeded();
                    } else {
                        final int fullblocks = length / DEFLATER_BLOCK_SIZE;
                        for (int i = 0; i < fullblocks; i++) {
                            def.setInput(b, offset + i * DEFLATER_BLOCK_SIZE,
                                    DEFLATER_BLOCK_SIZE);
                            deflateUntilInputIsNeeded();
                        }
                        final int done = fullblocks * DEFLATER_BLOCK_SIZE;
                        if (done < length) {
                            def.setInput(b, offset + done, length - done);
                            deflateUntilInputIsNeeded();
                        }
                    }
                }
            }
        } else {
            writeOut(b, offset, length);
            written += length;
        }
        crc.update(b, offset, length);
    }

    public void writeRaw(byte[] b, int offset, int length) throws IOException {
        writeOut(b, offset, length);
        written += length;
    }

    public void writeRaw(byte[] b) throws IOException {
        writeRaw(b, 0, b.length);
    }

    /**
     * Writes a single byte to ZIP entry.
     * <p>
     * <p>Delegates to the three arg method.</p>
     *
     * @param b the byte to write
     * @throws IOException on error
     * @since 1.14
     */
    public void write(int b) throws IOException {
        byte[] buff = new byte[1];
        buff[0] = (byte) (b & BYTE_MASK);
        write(buff, 0, 1);
    }

    /**
     * Closes this output stream and releases any system resources
     * associated with the stream.
     *
     * @throws IOException if an I/O error occurs.
     * @since 1.14
     */
    public void close() throws IOException {
        finish();

        if (raf != null) {
            raf.close();
        }
        if (out != null) {
            out.close();
        }
    }

    /**
     * Flushes this output stream and forces any buffered output bytes
     * to be written out to the stream.
     *
     * @throws IOException if an I/O error occurs.
     * @since 1.14
     */
    public void flush() throws IOException {
        if (out != null) {
            out.flush();
        }
    }

    /*
     * Various ZIP constants
     */
    /**
     * local file header signature
     *
     * @since 1.1
     */
    protected static final byte[] LFH_SIG = ZipLong.getBytes(0X04034B50L);
    /**
     * data descriptor signature
     *
     * @since 1.1
     */
    protected static final byte[] DD_SIG = ZipLong.getBytes(0X08074B50L);
    /**
     * central file header signature
     *
     * @since 1.1
     */
    protected static final byte[] CFH_SIG = ZipLong.getBytes(0X02014B50L);
    /**
     * end of central dir signature
     *
     * @since 1.1
     */
    protected static final byte[] EOCD_SIG = ZipLong.getBytes(0X06054B50L);

    /**
     * Writes next block of compressed data to the output stream.
     *
     * @throws IOException on error
     * @since 1.14
     */
    protected final void deflate() throws IOException {
        int len = def.deflate(buf, 0, buf.length);
        if (len > 0) {
            writeOut(buf, 0, len);
        }
    }

    /**
     * Writes the local file header entry
     *
     * @param ze the entry to write
     * @throws IOException on error
     * @since 1.1
     */
    protected void writeLocalFileHeader(ZipEntry ze) throws IOException {

        boolean encodable = zipEncoding.canEncode(ze.getName());

        final ZipEncoding entryEncoding;

        if (!encodable && fallbackToUTF8) {
            entryEncoding = ZipEncodingHelper.UTF8_ZIP_ENCODING;
        } else {
            entryEncoding = zipEncoding;
        }

        ByteBuffer name = entryEncoding.encode(ze.getName());

        if (createUnicodeExtraFields != UnicodeExtraFieldPolicy.NEVER) {

            if (createUnicodeExtraFields == UnicodeExtraFieldPolicy.ALWAYS
                    || !encodable) {
                ze.addExtraField(new UnicodePathExtraField(ze.getName(),
                        name.array(),
                        name.arrayOffset(),
                        name.limit()));
            }

            String comm = ze.getComment();
            if (comm != null && !"".equals(comm)) {

                boolean commentEncodable = this.zipEncoding.canEncode(comm);

                if (createUnicodeExtraFields == UnicodeExtraFieldPolicy.ALWAYS
                        || !commentEncodable) {
                    ByteBuffer commentB = entryEncoding.encode(comm);
                    ze.addExtraField(new UnicodeCommentExtraField(comm,
                            commentB.array(),
                            commentB.arrayOffset(),
                            commentB.limit())
                    );
                }
            }
        }

        offsets.put(ze, written);

        byte[] data = new byte[30 + name.limit()];

        putBytes(LFH_SIG, data, 0);
        written += WORD;

        //store method in local variable to prevent multiple method calls
        final int zipMethod = ze.getMethod();

        writeVersionNeededToExtractAndGeneralPurposeBits(data, 4, zipMethod,
                !encodable && fallbackToUTF8);
        written += WORD;

        // compression method
        putShort(zipMethod, data, 8);
        written += SHORT;

        // last mod. time and date
        putLong(toDosTime(ze.getTime()), data, 10);
        written += WORD;

        // CRC
        // compressed length
        // uncompressed length
        localDataStart = written;
        if (currentIsRawEntry) {
            putLong(ze.getCrc(), data, 14);
            putLong(ze.getCompressedSize(), data, 18);
            putLong(ze.getSize(), data, 22);
        } else if (zipMethod != DEFLATED && raf == null) {
            putLong(ze.getCrc(), data, 14);
            putLong(ze.getSize(), data, 18);
            putLong(ze.getSize(), data, 22);
        }
//        else {
//            writeOut(LZERO);
//            writeOut(LZERO);
//            writeOut(LZERO);
//        }
        // CheckStyle:MagicNumber OFF
        written += 12;
        // CheckStyle:MagicNumber ON

        // file name length
        putShort(name.limit(), data, 26);
        written += SHORT;

        // extra field length
        byte[] extra = ze.getLocalFileDataExtra();
        if (ze.getMethod() == STORED) {
            // ZipAlign对齐优化
            long len = written + SHORT + name.limit() + extra.length;
            if (len % 4 != 0) {
                int extraLen = 4 - (int) ((written + SHORT + name.limit()) % 4);
                extra = new byte[extraLen];
            }
        }
        putShort(extra.length, data, 28);
        written += SHORT;

        // file name
        putBytes(name.array(), name.arrayOffset(), name.limit(), data, 30);
        written += name.limit();

        writeOut(data);

        // extra field
        if (extra.length > 0) {
            writeOut(extra);
            written += extra.length;
        }

        dataStart = written;
    }

    private static void putBytes(byte[] value, int start, int length, byte[] buf, int offset) {
        for (int i = 0; i < length; i++) {
            buf[offset++] = value[start + i];
        }
    }

    private static void putBytes(byte[] value, byte[] buf, int offset) {
        putBytes(value, 0, value.length, buf, offset);
    }


    /**
     * Writes the data descriptor entry.
     *
     * @param ze the entry to write
     * @throws IOException on error
     * @since 1.1
     */
    protected void writeDataDescriptor(ZipEntry ze) throws IOException {
        if (ze.getMethod() != DEFLATED || raf != null) {
            return;
        }
        byte[] data = new byte[16];
        putBytes(DD_SIG, data, 0);
        putLong(entry.getCrc(), data, 4);
        putLong(entry.getCompressedSize(), data, 8);
        putLong(entry.getSize(), data, 12);
        writeOut(data);
        // CheckStyle:MagicNumber OFF
        written += 16;
        // CheckStyle:MagicNumber ON
    }

    /**
     * Writes the central file header entry.
     *
     * @param ze the entry to write
     * @throws IOException on error
     * @since 1.1
     */
    protected void writeCentralFileHeader(ZipEntry ze) throws IOException {
        final int zipMethod = ze.getMethod();
        final boolean encodable = zipEncoding.canEncode(ze.getName());

        final ZipEncoding entryEncoding;

        if (!encodable && fallbackToUTF8) {
            entryEncoding = ZipEncodingHelper.UTF8_ZIP_ENCODING;
        } else {
            entryEncoding = zipEncoding;
        }
        ByteBuffer name = entryEncoding.encode(ze.getName());

        byte[] extra = ze.getCentralDirectoryExtra();

        String comm = ze.getComment();
        if (comm == null) {
            comm = "";
        }

        ByteBuffer commentB = entryEncoding.encode(comm);


        byte[] data = new byte[46 + name.limit() + extra.length + commentB.limit()];

        putBytes(CFH_SIG, data, 0);
        written += WORD;

        // version made by
        // CheckStyle:MagicNumber OFF
        putShort((ze.getPlatform() << 8) | 20, data, 4);
        written += SHORT;


        writeVersionNeededToExtractAndGeneralPurposeBits(data, 6, zipMethod,
                !encodable && fallbackToUTF8);
        written += WORD;

        // compression method
        putShort(zipMethod, data, 10);
        written += SHORT;

        // last mod. time and date
        putLong(toDosTime(ze.getTime()), data, 12);
        written += WORD;

        // CRC
        // compressed length
        // uncompressed length
        putLong(ze.getCrc(), data, 16);
        putLong(ze.getCompressedSize(), data, 20);
        putLong(ze.getSize(), data, 24);
        // CheckStyle:MagicNumber OFF
        written += 12;
        // CheckStyle:MagicNumber ON

        // file name length

        putShort(name.limit(), data, 28);
        written += SHORT;

        // extra field length
        putShort(extra.length, data, 30);
        written += SHORT;

        // file comment length
        putShort(commentB.limit(), data, 32);
        written += SHORT;

        // disk number start
        written += SHORT;

        // internal file attributes
        putShort(ze.getInternalAttributes(), data, 36);
        written += SHORT;

        // external file attributes
        putLong(ze.getExternalAttributes(), data, 38);
        written += WORD;

        // relative offset of LFH
        putLong(offsets.get(ze), data, 42);
        written += WORD;

        // file name
        putBytes(name.array(), name.arrayOffset(), name.limit(), data, 46);
        written += name.limit();

        // extra field
        putBytes(extra, data, 46 + name.limit());
        written += extra.length;

        // file comment
        putBytes(commentB.array(), commentB.arrayOffset(), commentB.limit(),
                data, 46 + name.limit() + extra.length);
        written += commentB.limit();
        writeOut(data);
    }

    /**
     * Writes the &quot;End of central dir record&quot;.
     *
     * @throws IOException on error
     * @since 1.1
     */
    protected void writeCentralDirectoryEnd() throws IOException {
        ByteBuffer data = this.zipEncoding.encode(comment);

        byte[] buf = new byte[22 + data.limit()];
        putBytes(EOCD_SIG, buf, 0);
//        putBytes(ZERO, buf, 4);
//        putBytes(ZERO, buf, 6);
        putShort(entries.size(), buf, 8);
        putShort(entries.size(), buf, 10);
        putLong(cdLength, buf, 12);
        putLong(cdOffset, buf, 16);
        putShort(data.limit(), buf, 20);
        putBytes(data.array(), data.arrayOffset(), data.limit(), buf, 22);
        writeOut(buf);
//        writeOut(EOCD_SIG);//4
//
//        // disk numbers
//        writeOut(ZERO);//2
//        writeOut(ZERO);//2
//
//        // number of entries
//        byte[] num = ZipShort.getBytes(entries.size());
//        writeOut(num);//2
//        writeOut(num);//2
//
//        // length and location of CD
//        writeOut(ZipLong.getBytes(cdLength));//4
//        writeOut(ZipLong.getBytes(cdOffset));//4
//
//        // ZIP file comment
//        writeOut(ZipShort.getBytes(data.limit()));
//        writeOut(data.array(), data.arrayOffset(), data.limit());
    }

    /**
     * Smallest date/time ZIP can handle.
     *
     * @since 1.1
     */
//    private static final byte[] DOS_TIME_MIN = ZipLong.getBytes(0x00002100L);

    /**
     * Convert a Date object to a DOS date/time field.
     * <p>
     * <p>Stolen from InfoZip's <code>fileio.c</code></p>
     *
     * @param t number of milliseconds since the epoch
     * @return the date as a byte array
     * @since 1.26
     */
    @SuppressWarnings("deprecation")
    protected static long toDosTime(long t) {
        Date time = new Date(t);
        // CheckStyle:MagicNumberCheck OFF - I do not think that using constants
        //                                   here will improve the readablity
        int year = time.getYear() + 1900;
        if (year < 1980) {
            return 0x00002100L;
        }
        int month = time.getMonth() + 1;
        return ((year - 1980) << 25)
                | (month << 21)
                | (time.getDate() << 16)
                | (time.getHours() << 11)
                | (time.getMinutes() << 5)
                | (time.getSeconds() >> 1);
        // CheckStyle:MagicNumberCheck ON
    }

    /**
     * Write bytes to output or random access file.
     *
     * @param data the byte array to write
     * @throws IOException on error
     * @since 1.14
     */
    protected final void writeOut(byte[] data) throws IOException {
        writeOut(data, 0, data.length);
    }

    /**
     * Write bytes to output or random access file.
     *
     * @param data   the byte array to write
     * @param offset the start position to write from
     * @param length the number of bytes to write
     * @throws IOException on error
     * @since 1.14
     */
    protected final void writeOut(byte[] data, int offset, int length)
            throws IOException {
        if (raf != null) {
            raf.write(data, offset, length);
        } else {
            out.write(data, offset, length);
        }
    }

    /**
     * Assumes a negative integer really is a positive integer that
     * has wrapped around and re-creates the original value.
     *
     * @param i the value to treat as unsigned int.
     * @return the unsigned int as a long.
     * @since 1.34
     */
    protected static long adjustToLong(int i) {
        if (i < 0) {
            return 2 * ((long) Integer.MAX_VALUE) + 2 + i;
        } else {
            return i;
        }
    }

    private void deflateUntilInputIsNeeded() throws IOException {
        while (!def.needsInput()) {
            deflate();
        }
    }

    private void writeVersionNeededToExtractAndGeneralPurposeBits(
            final byte[] data, int offset,
            final int zipMethod, final boolean utfFallback)
            throws IOException {

        // CheckStyle:MagicNumber OFF
        int versionNeededToExtract = 10;
        int generalPurposeFlag = (useUTF8Flag || utfFallback) ? UFT8_NAMES_FLAG : 0;
        if (zipMethod == DEFLATED && raf == null) {
            // requires version 2 as we are going to store length info
            // in the data descriptor
            versionNeededToExtract = 20;
            // bit3 set to signal, we use a data descriptor
            generalPurposeFlag |= 8;
        }
        // CheckStyle:MagicNumber ON

        // version needed to extract
        putShort(versionNeededToExtract, data, offset);
        // general purpose bit flag
        putShort(generalPurposeFlag, data, offset + SHORT);
    }

    /**
     * enum that represents the possible policies for creating Unicode
     * extra fields.
     */
    public static final class UnicodeExtraFieldPolicy {
        /**
         * Always create Unicode extra fields.
         */
        public static final UnicodeExtraFieldPolicy ALWAYS =
                new UnicodeExtraFieldPolicy("always");
        /**
         * Never create Unicode extra fields.
         */
        public static final UnicodeExtraFieldPolicy NEVER =
                new UnicodeExtraFieldPolicy("never");
        /**
         * Create Unicode extra fields for filenames that cannot be
         * encoded using the specified encoding.
         */
        public static final UnicodeExtraFieldPolicy NOT_ENCODEABLE =
                new UnicodeExtraFieldPolicy("not encodeable");

        private final String name;

        private UnicodeExtraFieldPolicy(String n) {
            name = n;
        }

        public String toString() {
            return name;
        }
    }
}
