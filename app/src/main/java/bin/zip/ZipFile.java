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
import bin.zip.extrafield.AbstractUnicodeExtraField;
import bin.zip.extrafield.UnicodeCommentExtraField;
import bin.zip.extrafield.UnicodePathExtraField;

import java.io.*;
import java.util.*;
import java.util.zip.CRC32;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipException;

/**
 * Replacement for <code>java.util.ZipFile</code>.
 * <p>
 * <p>This class adds support for file name encodings other than UTF-8
 * (which is required to work on ZIP files created by native zip tools
 * and is able to skip a preamble like the one found in self
 * extracting archives.  Furthermore it returns instances of
 * <code>org.apache.tools.zip.ZipEntry</code> instead of
 * <code>java.util.zip.ZipEntry</code>.</p>
 * <p>
 * <p>It doesn't extend <code>java.util.zip.ZipFile</code> as it would
 * have to reimplement all methods anyway.  Like
 * <code>java.util.ZipFile</code>, it uses RandomAccessFile under the
 * covers and supports compressed and uncompressed entries.</p>
 * <p>
 * <p>The method signatures mimic the ones of
 * <code>java.util.zip.ZipFile</code>, with a couple of exceptions:
 * <p>
 * <ul>
 * <li>There is no getName method.</li>
 * <li>entries has been renamed to getEntries.</li>
 * <li>getEntries and getEntry return
 * <code>org.apache.tools.zip.ZipEntry</code> instances.</li>
 * <li>close is allowed to throw IOException.</li>
 * </ul>
 */
public class ZipFile implements Closeable {
    private static final int HASH_SIZE = 509;
    private static final int SHORT = 2;
    private static final int WORD = 4;
    private static final int NIBLET_MASK = 0x0f;
    private static final int BYTE_SHIFT = 8;
    private static final int POS_0 = 0;
    private static final int POS_1 = 1;
    private static final int POS_2 = 2;
    private static final int POS_3 = 3;

    /**
     * Maps ZipEntrys to Longs, recording the offsets of the local
     * file headers.
     */
    private final Map<ZipEntry, OffsetEntry> entries = new HashMap<>(HASH_SIZE);

    /**
     * Maps String to ZipEntrys, name -> actual entry.
     */
    private final Map<String, ZipEntry> nameMap = new HashMap<>(HASH_SIZE);

    private static final class OffsetEntry {
        private long headerOffset = -1;
        private long dataOffset = -1;
    }

    /**
     * The zip encoding to use for filenames and the file comment.
     */
    private final ZipEncoding zipEncoding;

    /**
     * The actual data source.
     */
    private final RandomAccessFile archive;

    /**
     * Whether to look for and use Unicode extra fields.
     */
    private final boolean useUnicodeExtraFields;

    /**
     * Opens the given file for reading, assuming the platform's
     * native encoding for file names.
     *
     * @param f the archive.
     * @throws IOException if an error occurs while reading the file.
     */
    public ZipFile(File f) throws IOException {
        this(f, null);
    }

    /**
     * Opens the given file for reading, assuming the platform's
     * native encoding for file names.
     *
     * @param name name of the archive.
     * @throws IOException if an error occurs while reading the file.
     */
    public ZipFile(String name) throws IOException {
        this(new File(name), null);
    }

    /**
     * Opens the given file for reading, assuming the specified
     * encoding for file names, scanning unicode extra fields.
     *
     * @param name     name of the archive.
     * @param encoding the encoding to use for file names
     * @throws IOException if an error occurs while reading the file.
     */
    public ZipFile(String name, String encoding) throws IOException {
        this(new File(name), encoding, true);
    }

    /**
     * Opens the given file for reading, assuming the specified
     * encoding for file names and scanning for unicode extra fields.
     *
     * @param f        the archive.
     * @param encoding the encoding to use for file names, use null
     *                 for the platform's default encoding
     * @throws IOException if an error occurs while reading the file.
     */
    public ZipFile(File f, String encoding) throws IOException {
        this(f, encoding, true);
    }

    /**
     * Opens the given file for reading, assuming the specified
     * encoding for file names.
     *
     * @param f                     the archive.
     * @param encoding              the encoding to use for file names, use null
     *                              for the platform's default encoding
     * @param useUnicodeExtraFields whether to use InfoZIP Unicode
     *                              Extra Fields (if present) to set the file names.
     * @throws IOException if an error occurs while reading the file.
     */
    public ZipFile(File f, String encoding, boolean useUnicodeExtraFields)
            throws IOException {
        this.zipEncoding = ZipEncodingHelper.getZipEncoding(encoding);
        this.useUnicodeExtraFields = useUnicodeExtraFields;
        archive = new RandomAccessFile(f, "r");
        boolean success = false;
        try {
            Map<ZipEntry, NameAndComment> entriesWithoutUTF8Flag = populateFromCentralDirectory();
            resolveLocalFileHeaderData(entriesWithoutUTF8Flag);
            success = true;
        } finally {
            if (!success) {
                try {
                    archive.close();
                } catch (IOException e2) {
                    // swallow, throw the original exception instead
                }
            }
        }
    }

    public ZipEncoding getZipEncoding() {
        return zipEncoding;
    }

    public int getEntrySize() {
        return entries.size();
    }


    /**
     * The encoding to use for filenames and the file comment.
     *
     * @return null if using the platform's default character encoding.
     */
    public String getEncoding() {
        return zipEncoding.getEncoding();
    }

    /**
     * Closes the archive.
     *
     * @throws IOException if an error occurs closing the archive.
     */
    @Override
    public void close() throws IOException {
        archive.close();
    }

    /**
     * close a zipfile quietly; throw no io fault, do nothing
     * on a null parameter
     *
     * @param zipfile file to close, can be null
     */
    public static void closeQuietly(ZipFile zipfile) {
        if (zipfile != null) {
            try {
                zipfile.close();
            } catch (IOException e) {
                //ignore
            }
        }
    }

    /**
     * Returns all entries.
     *
     * @return all entries as {@link ZipEntry} instances
     */
    public Enumeration<ZipEntry> getEntries() {
        return Collections.enumeration(entries.keySet());
    }

    /**
     * Returns a named entry - or <code>null</code> if no entry by
     * that name exists.
     *
     * @param name name of the entry.
     * @return the ZipEntry corresponding to the given name - or
     * <code>null</code> if not present.
     */
    public ZipEntry getEntry(String name) {
        return nameMap.get(name);
    }

    /**
     * Returns an InputStream for reading the contents of the given entry.
     *
     * @param ze the entry to get the stream for.
     * @return a stream to read the entry from.
     * @throws IOException  if unable to create an input stream from the zipenty
     * @throws ZipException if the zipentry has an unsupported
     *                      compression method
     */
    public InputStream getInputStream(ZipEntry ze)
            throws IOException {
        OffsetEntry offsetEntry = entries.get(ze);
        if (offsetEntry == null) {
            return null;
        }
        long start = offsetEntry.dataOffset;
        BoundedInputStream bis =
                new BoundedInputStream(start, ze.getCompressedSize());
        switch (ze.getMethod()) {
            case ZipEntry.STORED:
                return bis;
            case ZipEntry.DEFLATED:
                bis.addDummy();
                final Inflater inflater = new Inflater(true);
                return new InflaterInputStream(bis, inflater) {
                    public void close() throws IOException {
                        super.close();
                        inflater.end();
                    }
                };
            default:
                throw new ZipException("Found unsupported compression method "
                        + ze.getMethod());
        }
    }

    public InputStream getRawInputStream(ZipEntry ze)
            throws IOException {
        OffsetEntry offsetEntry = entries.get(ze);
        if (offsetEntry == null)
            return null;
        return new BoundedInputStream(offsetEntry.dataOffset, ze.getCompressedSize());
    }

    private static final int CFH_LEN =
        /* version made by                 */ SHORT
        /* version needed to extract       */ + SHORT
        /* general purpose bit flag        */ + SHORT
        /* compression method              */ + SHORT
        /* last mod file time              */ + SHORT
        /* last mod file date              */ + SHORT
        /* crc-32                          */ + WORD
        /* compressed size                 */ + WORD
        /* uncompressed size               */ + WORD
        /* filename length                 */ + SHORT
        /* extra field length              */ + SHORT
        /* file comment length             */ + SHORT
        /* disk number start               */ + SHORT
        /* internal file attributes        */ + SHORT
        /* external file attributes        */ + WORD
        /* relative offset of local header */ + WORD;

    /**
     * Reads the central directory of the given archive and populates
     * the internal tables with ZipEntry instances.
     * <p>
     * <p>The ZipEntrys will know all data that can be obtained from
     * the central directory alone, but not the data that requires the
     * local file header or additional data to be read.</p>
     *
     * @return a Map&lt;ZipEntry, NameAndComment>&gt; of
     * zipentries that didn't have the language encoding flag set when
     * read.
     */
    private Map<ZipEntry, NameAndComment> populateFromCentralDirectory()
            throws IOException {
        HashMap<ZipEntry, NameAndComment> noUTF8Flag = new HashMap<>();

        positionAtCentralDirectory();

        byte[] cfh = new byte[CFH_LEN];

        byte[] signatureBytes = new byte[WORD];
        archive.readFully(signatureBytes);
        long sig = ZipLong.getValue(signatureBytes);
        final long cfhSig = ZipLong.getValue(ZipOutputStream.CFH_SIG);
        if (sig != cfhSig && startsWithLocalFileHeader()) {
            throw new IOException("central directory is empty, can't expand"
                    + " corrupt archive.");
        }
        while (sig == cfhSig) {
            archive.readFully(cfh);
            int off = 0;
            ZipEntry ze = new ZipEntry();

            int versionMadeBy = ZipShort.getValue(cfh, off);
            off += SHORT;
            ze.setPlatform((versionMadeBy >> BYTE_SHIFT) & NIBLET_MASK);

            off += SHORT; // skip version info

            final int generalPurposeFlag = ZipShort.getValue(cfh, off);
            final boolean hasUTF8Flag =
                    (generalPurposeFlag & ZipOutputStream.UFT8_NAMES_FLAG) != 0;
            final ZipEncoding entryEncoding =
                    hasUTF8Flag ? ZipEncodingHelper.UTF8_ZIP_ENCODING : zipEncoding;

            off += SHORT;

            //noinspection MagicConstant
            ze.setMethod(ZipShort.getValue(cfh, off));
            off += SHORT;

            // FIXME this is actually not very cpu cycles friendly as we are converting from
            // dos to java while the underlying Sun implementation will convert
            // from java to dos time for internal storage...
            long time = dosToJavaTime(ZipLong.getValue(cfh, off));
            ze.setTime(time);
            off += WORD;

            ze.setCrc(ZipLong.getValue(cfh, off));
            off += WORD;

            ze.setCompressedSize(ZipLong.getValue(cfh, off));
            off += WORD;

            ze.setSize(ZipLong.getValue(cfh, off));
            off += WORD;

            int fileNameLen = ZipShort.getValue(cfh, off);
            off += SHORT;

            int extraLen = ZipShort.getValue(cfh, off);
            off += SHORT;

            int commentLen = ZipShort.getValue(cfh, off);
            off += SHORT;

            off += SHORT; // disk number

            ze.setInternalAttributes(ZipShort.getValue(cfh, off));
            off += SHORT;

            ze.setExternalAttributes(ZipLong.getValue(cfh, off));
            off += WORD;

            byte[] fileName = new byte[fileNameLen];
            archive.readFully(fileName);
            ze.setName(entryEncoding.decode(fileName));

            // LFH offset,
            OffsetEntry offset = new OffsetEntry();
            offset.headerOffset = ZipLong.getValue(cfh, off);
            // data offset will be filled later
            entries.put(ze, offset);

            nameMap.put(ze.getName(), ze);

            byte[] cdExtraData = new byte[extraLen];
            archive.readFully(cdExtraData);
            ze.setCentralDirectoryExtra(cdExtraData);

            byte[] comment = new byte[commentLen];
            archive.readFully(comment);
            ze.setComment(entryEncoding.decode(comment));

            archive.readFully(signatureBytes);
            sig = ZipLong.getValue(signatureBytes);

            if (!hasUTF8Flag && useUnicodeExtraFields) {
                noUTF8Flag.put(ze, new NameAndComment(fileName, comment));
            }
        }
        return noUTF8Flag;
    }

    private static final int MIN_EOCD_SIZE =
        /* end of central dir signature    */ WORD
        /* number of this disk             */ + SHORT
        /* number of the disk with the     */
        /* start of the central directory  */ + SHORT
        /* total number of entries in      */
        /* the central dir on this disk    */ + SHORT
        /* total number of entries in      */
        /* the central dir                 */ + SHORT
        /* size of the central directory   */ + WORD
        /* offset of start of central      */
        /* directory with respect to       */
        /* the starting disk number        */ + WORD
        /* zipfile comment length          */ + SHORT;

    private static final int MAX_EOCD_SIZE = MIN_EOCD_SIZE
        /* maximum length of zipfile comment */ + 0xFFFF;

    private static final int CFD_LOCATOR_OFFSET =
        /* end of central dir signature    */ WORD
        /* number of this disk             */ + SHORT
        /* number of the disk with the     */
        /* start of the central directory  */ + SHORT
        /* total number of entries in      */
        /* the central dir on this disk    */ + SHORT
        /* total number of entries in      */
        /* the central dir                 */ + SHORT
        /* size of the central directory   */ + WORD;

    /**
     * Searches for the &quot;End of central dir record&quot;, parses
     * it and positions the stream at the first central directory
     * record.
     */
    private void positionAtCentralDirectory()
            throws IOException {
        boolean found = false;
        long off = archive.length() - MIN_EOCD_SIZE;
        final long stopSearching =
                Math.max(0L, archive.length() - MAX_EOCD_SIZE);
        if (off >= 0) {
            final byte[] sig = ZipOutputStream.EOCD_SIG;
            for (; off >= stopSearching; off--) {
                archive.seek(off);
                int curr = archive.read();
                if (curr == -1) {
                    break;
                }
                if (curr == sig[POS_0]) {
                    curr = archive.read();
                    if (curr == sig[POS_1]) {
                        curr = archive.read();
                        if (curr == sig[POS_2]) {
                            curr = archive.read();
                            if (curr == sig[POS_3]) {
                                found = true;
                                break;
                            }
                        }
                    }
                }
            }
        }
        if (!found) {
            throw new ZipException("archive is not a ZIP archive");
        }
        archive.seek(off + CFD_LOCATOR_OFFSET);
        byte[] cfdOffset = new byte[WORD];
        archive.readFully(cfdOffset);
        archive.seek(ZipLong.getValue(cfdOffset));
    }

    /**
     * Number of bytes in local file header up to the &quot;length of
     * filename&quot; entry.
     */
    private static final long LFH_OFFSET_FOR_FILENAME_LENGTH =
        /* local file header signature     */ WORD
        /* version needed to extract       */ + SHORT
        /* general purpose bit flag        */ + SHORT
        /* compression method              */ + SHORT
        /* last mod file time              */ + SHORT
        /* last mod file date              */ + SHORT
        /* crc-32                          */ + WORD
        /* compressed size                 */ + WORD
        /* uncompressed size               */ + WORD;

    /**
     * Walks through all recorded entries and adds the data available
     * from the local file header.
     * <p>
     * <p>Also records the offsets for the data to read from the
     * entries.</p>
     */
    private void resolveLocalFileHeaderData(Map<ZipEntry, NameAndComment> entriesWithoutUTF8Flag)
            throws IOException {
        Enumeration<ZipEntry> e = Collections.enumeration(new HashSet<>(entries.keySet()));
        while (e.hasMoreElements()) {
            ZipEntry ze = e.nextElement();
            OffsetEntry offsetEntry = entries.get(ze);
            long offset = offsetEntry.headerOffset;
            archive.seek(offset + LFH_OFFSET_FOR_FILENAME_LENGTH);
            byte[] b = new byte[SHORT];
            archive.readFully(b);
            int fileNameLen = ZipShort.getValue(b);
            archive.readFully(b);
            int extraFieldLen = ZipShort.getValue(b);
            int lenToSkip = fileNameLen;
            while (lenToSkip > 0) {
                int skipped = archive.skipBytes(lenToSkip);
                if (skipped <= 0) {
                    throw new RuntimeException("failed to skip file name in"
                            + " local file header");
                }
                lenToSkip -= skipped;
            }
            byte[] localExtraData = new byte[extraFieldLen];
            archive.readFully(localExtraData);
            ze.setExtra(localExtraData);
            /*dataOffsets.put(ze,
                            new Long(offset + LFH_OFFSET_FOR_FILENAME_LENGTH
                                     + SHORT + SHORT + fileNameLen + extraFieldLen));
            */
            offsetEntry.dataOffset = offset + LFH_OFFSET_FOR_FILENAME_LENGTH
                    + SHORT + SHORT + fileNameLen + extraFieldLen;

            if (entriesWithoutUTF8Flag.containsKey(ze)) {
                // changing the name of a ZipEntry is going to change
                // the hashcode
                // - see https://issues.apache.org/jira/browse/COMPRESS-164
                entries.remove(ze);
                setNameAndCommentFromExtraFields(ze, entriesWithoutUTF8Flag.get(ze));
                entries.put(ze, offsetEntry);
            }
        }
    }

    /**
     * Convert a DOS date/time field to a Date object.
     *
     * @param zipDosTime contains the stored DOS time.
     * @return a Date instance corresponding to the given time.
     */
    protected static Date fromDosTime(ZipLong zipDosTime) {
        long dosTime = zipDosTime.getValue();
        return new Date(dosToJavaTime(dosTime));
    }

    /*
     * Converts DOS time to Java time (number of milliseconds since epoch).
     */
    private static long dosToJavaTime(long dosTime) {
        Calendar cal = Calendar.getInstance();
        // CheckStyle:MagicNumberCheck OFF - no point
        cal.set(Calendar.YEAR, (int) ((dosTime >> 25) & 0x7f) + 1980);
        cal.set(Calendar.MONTH, (int) ((dosTime >> 21) & 0x0f) - 1);
        cal.set(Calendar.DATE, (int) (dosTime >> 16) & 0x1f);
        cal.set(Calendar.HOUR_OF_DAY, (int) (dosTime >> 11) & 0x1f);
        cal.set(Calendar.MINUTE, (int) (dosTime >> 5) & 0x3f);
        cal.set(Calendar.SECOND, (int) (dosTime << 1) & 0x3e);
        // CheckStyle:MagicNumberCheck ON
        return cal.getTime().getTime();
    }

    /**
     * Checks whether the archive starts with a LFH.  If it doesn't,
     * it may be an empty archive.
     */
    private boolean startsWithLocalFileHeader() throws IOException {
        archive.seek(0);
        final byte[] start = new byte[WORD];
        archive.readFully(start);
        for (int i = 0; i < start.length; i++) {
            if (start[i] != ZipOutputStream.LFH_SIG[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * If the entry has Unicode*ExtraFields and the CRCs of the
     * names/comments match those of the extra fields, transfer the
     * known Unicode values from the extra field.
     */
    private void setNameAndCommentFromExtraFields(ZipEntry ze,
                                                  NameAndComment nc) {
        UnicodePathExtraField name = (UnicodePathExtraField)
                ze.getExtraField(UnicodePathExtraField.UPATH_ID);
        String originalName = ze.getName();
        String newName = getUnicodeStringIfOriginalMatches(name, nc.name);
        if (newName != null && !originalName.equals(newName)) {
            ze.setName(newName);
            nameMap.remove(originalName);
            nameMap.put(newName, ze);
        }

        if (nc.comment != null && nc.comment.length > 0) {
            UnicodeCommentExtraField cmt = (UnicodeCommentExtraField)
                    ze.getExtraField(UnicodeCommentExtraField.UCOM_ID);
            String newComment =
                    getUnicodeStringIfOriginalMatches(cmt, nc.comment);
            if (newComment != null) {
                ze.setComment(newComment);
            }
        }
    }

    /**
     * If the stored CRC matches the one of the given name, return the
     * Unicode name of the given field.
     * <p>
     * <p>If the field is null or the CRCs don't match, return null
     * instead.</p>
     */
    private String getUnicodeStringIfOriginalMatches(AbstractUnicodeExtraField f,
                                                     byte[] orig) {
        if (f != null) {
            CRC32 crc32 = new CRC32();
            crc32.update(orig);
            long origCRC32 = crc32.getValue();

            if (origCRC32 == f.getNameCRC32()) {
                try {
                    return ZipEncodingHelper
                            .UTF8_ZIP_ENCODING.decode(f.getUnicodeName());
                } catch (IOException ex) {
                    // UTF-8 unsupported?  should be impossible the
                    // Unicode*ExtraField must contain some bad bytes

                    // TODO log this anywhere?
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * InputStream that delegates requests to the underlying
     * RandomAccessFile, making sure that only bytes from a certain
     * range can be read.
     */
    private class BoundedInputStream extends InputStream {
        private long remaining;
        private long loc;
        private boolean addDummyByte = false;

        BoundedInputStream(long start, long remaining) {
            this.remaining = remaining;
            loc = start;
        }

        public int read() throws IOException {
            if (remaining-- <= 0) {
                if (addDummyByte) {
                    addDummyByte = false;
                    return 0;
                }
                return -1;
            }
            synchronized (archive) {
                archive.seek(loc++);
                return archive.read();
            }
        }

        public int read(byte[] b, int off, int len) throws IOException {
            if (remaining <= 0) {
                if (addDummyByte) {
                    addDummyByte = false;
                    b[off] = 0;
                    return 1;
                }
                return -1;
            }

            if (len <= 0) {
                return 0;
            }

            if (len > remaining) {
                len = (int) remaining;
            }
            int ret;
            synchronized (archive) {
                archive.seek(loc);
                ret = archive.read(b, off, len);
            }
            if (ret > 0) {
                loc += ret;
                remaining -= ret;
            }
            return ret;
        }

        /**
         * Inflater needs an extra dummy byte for nowrap - see
         * Inflater's javadocs.
         */
        void addDummy() {
            addDummyByte = true;
        }
    }

    private static final class NameAndComment {
        private final byte[] name;
        private final byte[] comment;

        private NameAndComment(byte[] name, byte[] comment) {
            this.name = name;
            this.comment = comment;
        }
    }
}
