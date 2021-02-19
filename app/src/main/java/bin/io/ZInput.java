package bin.io;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public final class ZInput {
    protected final DataInputStream dis;
    protected final byte[] work;
    private int size;

    public ZInput(InputStream in) throws IOException {
        dis = new DataInputStream(in);
        work = new byte[8];
        size = 0;
    }

    public int getOffset() throws IOException {
        return size;
    }

    public void skipByOffset(int offset) throws IOException {
        offset -= getOffset();
        if (offset > 0) {
            skipBytes(offset);
        }
    }

    public void close() throws IOException {
        dis.close();
    }

    public int available() throws IOException {
        return dis.available();
    }

    public final boolean readBoolean() throws IOException {
        size++;
        return dis.readBoolean();
    }

    public final byte readByte() throws IOException {
        size++;
        return dis.readByte();
    }

    public final char readChar() throws IOException {
        dis.readFully(work, 0, 2);
        size += 2;
        return (char) ((work[1] & 0xFF) << 8 | work[0] & 0xFF);
    }

    public final double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    public int[] readIntArray(int length) throws IOException {
        int[] array = new int[length];
        for (int i = 0; i < length; i++)
            array[i] = readInt();
        return array;
    }

    public void skipInt() throws IOException {
        skipBytes(4);
    }

    public void skipCheckChunkTypeInt(int expected, int possible) throws IOException {
        int got = readInt();

        if (got == possible) {
            skipCheckChunkTypeInt(expected, -1);
        } else if (got != expected) {
            throw new IOException(String.format("Expected: 0x%08x, got: 0x%08x", expected, got));
        }
    }

    public void skipCheckInt(int expected) throws IOException {
        int got = readInt();
        if (got != expected)
            throw new IOException(String.format(
                    "Expected: 0x%08x, got: 0x%08x", expected, got));
    }

    public void skipCheckShort(short expected) throws IOException {
        short got = readShort();
        if (got != expected)
            throw new IOException(
                    String.format(
                            "Expected: 0x%08x, got: 0x%08x", expected, got));
    }

    public void skipCheckByte(byte expected) throws IOException {
        byte got = readByte();
        if (got != expected)
            throw new IOException(String.format(
                    "Expected: 0x%08x, got: 0x%08x", expected, got));
    }


    public int read(byte[] b, int a, int len) throws IOException {
        int r = dis.read(b, a, len);
        size += r;
        return r;
    }

    public final float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    public final void readFully(byte[] ba) throws IOException {
        dis.readFully(ba, 0, ba.length);
        size += ba.length;
    }

    public final void readFully(byte[] ba, int off, int len) throws IOException {
        dis.readFully(ba, off, len);
        size += len;
    }

    public final int readInt() throws IOException {
        dis.readFully(work, 0, 4);
        size += 4;
        return work[3] << 24 | (work[2] & 0xFF) << 16 | (work[1] & 0xFF) << 8
                | work[0] & 0xFF;
    }

    public final long readLong() throws IOException {
        dis.readFully(work, 0, 8);
        size += 8;
        return (long) work[7] << 56 | ((long) work[6] & 0xFF) << 48 | ((long) work[5] & 0xFF) << 40
                | ((long) work[4] & 0xFF) << 32 | ((long) work[3] & 0xFF) << 24
                | ((long) work[2] & 0xFF) << 16 | ((long) work[1] & 0xFF) << 8 | (long) work[0]
                & 0xFF;
    }

    public final short readShort() throws IOException {
        dis.readFully(work, 0, 2);
        size += 2;
        return (short) ((work[1] & 0xFF) << 8 | work[0] & 0xFF);
    }

//    public final String readUTF() throws IOException {
//        return dis.readUTF();
//    }

//    public final int readUnsignedByte() throws IOException {
//        return dis.readUnsignedByte();
//    }

    public final int readUnsignedShort() throws IOException {
        dis.readFully(work, 0, 2);
        size += 2;
        return (work[1] & 0xFF) << 8 | work[0] & 0xFF;
    }

    public final int skipBytes(int n) throws IOException {
        size += n;
        return dis.skipBytes(n);
    }

    public String readNullEndedString(int length, boolean fixed)
            throws IOException {
        StringBuilder string = new StringBuilder(16);
        while (length-- != 0) {
            short ch = readShort();
            if (ch == 0) {
                break;
            }
            string.append((char) ch);
        }
        if (fixed) {
            skipBytes(length * 2);
        }

        return string.toString();
    }
}
