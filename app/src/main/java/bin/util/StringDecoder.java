package bin.util;

import bin.io.ZInput;
import bin.io.ZOutput;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.List;

public class StringDecoder {
    private String[] m_strings;
    private int[] m_styleOffsets;
    private int[] m_styles;
    private boolean m_isUTF8;
    private int styleOffsetCount;
    private int stylesOffset;
    private int flags;
    private int chunkSize;
    private int m_strings_size;


    public boolean isUtf8() {
        return m_isUTF8;
    }

    private static final CharsetDecoder UTF16LE_DECODER = Charset.forName(
            "UTF-16LE").newDecoder();

    private static final CharsetDecoder UTF8_DECODER = Charset.forName("UTF-8")
            .newDecoder();

    private static final int CHUNK_STRINGPOOL_TYPE = 0x001C0001;
    private static final int CHUNK_NULL_TYPE = 0x00000000;
    public static final int IS_UTF8 = 0x100;

    public static StringDecoder read(ZInput mIn) throws IOException {
        mIn.skipCheckChunkTypeInt(CHUNK_STRINGPOOL_TYPE, CHUNK_NULL_TYPE);
        StringDecoder block = new StringDecoder();
        int chunkSize = block.chunkSize = mIn.readInt();

        // ResStringPool_header
        int stringCount = mIn.readInt();
        int styleCount = block.styleOffsetCount = mIn.readInt();
        int flags = block.flags = mIn.readInt();
        int stringsOffset = mIn.readInt();
        int stylesOffset = block.stylesOffset = mIn.readInt();

        block.m_isUTF8 = (flags & IS_UTF8) != 0;
        int[] m_stringOffsets = mIn.readIntArray(stringCount);

        if (styleCount != 0) {
            block.m_styleOffsets = mIn.readIntArray(styleCount);
        }
        int size = ((stylesOffset == 0) ? chunkSize : stylesOffset) - stringsOffset;
        byte[] data = new byte[size];
        mIn.readFully(data);
        block.m_strings_size = size;

        if (stylesOffset != 0) {
            size = (chunkSize - stylesOffset);
            block.m_styles = mIn.readIntArray(size / 4);

            // read remaining bytes
            int remaining = size % 4;
            if (remaining >= 1) {
                while (remaining-- > 0) {
                    mIn.readByte();
                }
            }
        }
        // System.out.println();

        int i = 0;
        block.m_strings = new String[m_stringOffsets.length];
        for (int offset : m_stringOffsets) {
            int length;
            if (!block.m_isUTF8) {
                length = getShort(data, offset) * 2;
                offset += 2;
            } else {
                offset += getVarint(data, offset)[1];
                int[] varint = getVarint(data, offset);
                offset += varint[1];
                length = varint[0];
            }
            block.m_strings[i++] = decodeString(offset, length, block.m_isUTF8,
                    data);
        }
        data = null;
        return block;
    }

    /**
     * Finds index of the string. Returns -1 if the string was not found.
     */
    public int find(String string) {
        if (string == null) {
            return -1;
        }
        for (int i = 0; i < m_strings.length; i++) {
            if (getString(i).equals(string))
                return i;
        }
        return -1;
    }

    public void getStrings(List<String> list) {
        int size = getSize();
        for (int i = 0; i < size; i++)
            list.add(getString(i));
    }

    public void write(ZOutput out) throws IOException {
        // List<String> list = new ArrayList<String>(getSize());
        // getStrings(list);
        write(m_strings, out);
    }

    public void write(String[] s, ZOutput out) throws IOException {
        ByteArrayOutputStream outBuf = new ByteArrayOutputStream();
        ZOutput led = new ZOutput(outBuf);
        // stringCount
        int size = s.length;

        // m_stringOffsets
        int[] offset = new int[size];
        int len = 0;

        // m_strings
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        ZOutput mStrings = new ZOutput(bOut);

        if (this.m_isUTF8) {
            for (int i = 0; i < size; i++) {
                offset[i] = len;
                String var = s[i];
                char[] charBuf = var.toCharArray();
                byte[] b = getVarBytes(charBuf.length);
                mStrings.writeFully(b);
                len += b.length;
                byte[] buf = var.getBytes("UTF-8");
                b = getVarBytes(buf.length);
                mStrings.writeFully(b);
                len += b.length;
                mStrings.writeFully(buf);
                len += buf.length;
                mStrings.writeByte(0);
                len += 1;
            }
        } else {
            for (int i = 0; i < size; i++) {
                offset[i] = len;
                String var = s[i];
                char[] charBuf = var.toCharArray();
                mStrings.writeShort((short) charBuf.length);
                for (char c : charBuf)
                    mStrings.writeChar(c);
                mStrings.writeShort((short) 0);
                len += charBuf.length * 2 + 4;
            }
        }

        int m_strings_size = bOut.size();
        int size_mod = m_strings_size % 4;// m_strings_size%4
        // padding 0
        if (size_mod != 0) {
            for (int i = 0; i < 4 - size_mod; i++)
                bOut.write(0);
            m_strings_size += 4 - size_mod;
        }
        byte[] m_strings = bOut.toByteArray();

        // System.out.println("string chunk size: " + chunkSize);

        led.writeInt(size);
        led.writeInt(styleOffsetCount);
        led.writeInt(flags);
        // led.writeInt(0x010);

        int stringsOffset = 28 + (size + styleOffsetCount) * 4;
        led.writeInt(stringsOffset);
        int i = m_strings_size - this.m_strings_size;
        led.writeInt(stylesOffset == 0 ? 0 : stylesOffset + i * 4);

        led.writeIntArray(offset);
        if (styleOffsetCount != 0)
            for (int j : m_styleOffsets)
                led.writeInt(j);

        led.writeFully(m_strings);

        if (m_styles != null)
            // System.out.println("write m_styles");
            led.writeIntArray(m_styles);
        out.writeInt(CHUNK_STRINGPOOL_TYPE);

        byte[] b = outBuf.toByteArray();
        outBuf.close();
        led.close();
        out.writeInt(b.length + 8);
        out.writeFully(b);
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public void setString(int index, String s) {
        m_strings[index] = s;
    }

    public String getString(int index) {
        if (index >= 0)
            return m_strings[index];
        return null;
    }

    private static int[] getVarint(byte[] array, int offset) {
        if ((array[offset] & 0x80) == 0)
            return new int[]{array[offset] & 0x7f, 1};
        else {
            return new int[]{
                    ((array[offset] & 0x7f) << 8) | array[offset + 1] & 0xFF, 2};
        }

        // int val = array[offset];
        // boolean more = (val & 0x80) != 0;//1000 000
        // val &= 127;
        //
        // if (!more)
        // return new int[] { val, 1 };
        // return new int[] { val << 8 | array[offset + 1] & 0xFF, 2 };
    }

    protected static byte[] getVarBytes(int val) {
        if ((val & 0x7f) == val)// 111 1111
            return new byte[]{(byte) val};
        else {
            byte[] b = new byte[2];
            b[0] = (byte) (val >>> 8 | 0x80);
            b[1] = (byte) (val & 0xff);
            return b;
        }
    }

    public int getSize() {
        return m_strings.length;
    }

    private static String decodeString(int offset, int length, boolean utf8,
                                       byte[] data) {
        try {
            return (utf8 ? UTF8_DECODER : UTF16LE_DECODER).decode(
                    ByteBuffer.wrap(data, offset, length)).toString();
        } catch (CharacterCodingException ignored) {
        }
        return null;
    }

    private static int getShort(byte[] array, int offset) {
        return (array[offset + 1] & 0xFF) << 8 | array[offset] & 0xFF;
    }

}
