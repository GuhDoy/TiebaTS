package bin.xml.decode;

import bin.io.ZInput;
import bin.io.ZOutput;
import bin.util.StringDecoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class AXmlDecoder {
    private static final int AXML_CHUNK_TYPE = 0x00080003;
    public StringDecoder mTableStrings;
    private final ZInput mIn;
    byte[] data;

    private void readStrings() throws IOException {
        int type = mIn.readInt();
        checkChunk(type, AXML_CHUNK_TYPE);
        mIn.readInt();// Chunk size
        mTableStrings = StringDecoder.read(this.mIn);

        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        byte[] buf = new byte[2048];
        int num;
        while ((num = mIn.read(buf, 0, 2048)) != -1)
            byteOut.write(buf, 0, num);
        data = byteOut.toByteArray();
        mIn.close();
        byteOut.close();
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    private AXmlDecoder(ZInput in) {
        this.mIn = in;
    }

    public static AXmlDecoder decode(InputStream input) throws IOException {
        AXmlDecoder axml = new AXmlDecoder(new ZInput(input));
        axml.readStrings();
        return axml;
    }

    public void write(List<String> list, OutputStream out) throws IOException {
        write(list, new ZOutput(out));
    }

    public void write(List<String> list, ZOutput out) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZOutput buf = new ZOutput(baos);
        String[] array = new String[list.size()];
        list.toArray(array);
        mTableStrings.write(array, buf);
        buf.writeFully(data);
        // write out
        out.writeInt(AXML_CHUNK_TYPE);
        out.writeInt(baos.size() + 8);
        out.writeFully(baos.toByteArray());
        buf.close();
    }

    public void write(ZOutput out) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZOutput buf = new ZOutput(baos);
        mTableStrings.write(buf);
        buf.writeFully(data);
        // write out
        out.writeInt(AXML_CHUNK_TYPE);
        out.writeInt(baos.size() + 8);
        out.writeFully(baos.toByteArray());
        baos.reset();
        buf.close();
    }

    public byte[] encode() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZOutput buf = new ZOutput(baos);
        mTableStrings.write(buf);
        buf.writeFully(data);

        byte[] bytes = baos.toByteArray();
        baos.reset();
        // write out
        buf.writeInt(AXML_CHUNK_TYPE);
        buf.writeInt(bytes.length + 8);
        buf.writeFully(bytes);
        return baos.toByteArray();
    }


    private void checkChunk(int type, int expectedType) throws IOException {
        if (type != expectedType)
            throw new IOException(String.format("Invalid chunk type: expected=0x%08x, got=0x%08x", expectedType, type));
    }

}
