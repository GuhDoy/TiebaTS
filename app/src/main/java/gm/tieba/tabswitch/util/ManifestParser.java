package gm.tieba.tabswitch.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import bin.xml.decode.AXmlDecoder;
import bin.xml.decode.AXmlResourceParser;
import bin.xml.decode.XmlPullParser;

public class ManifestParser {
    public static String packageName;
    public static String applicationName;
    private static boolean isCustomizedApplication = false;

    public static byte[] parseManifest(InputStream is, String customApplicationName) throws IOException {
        AXmlDecoder axml = AXmlDecoder.decode(is);
        AXmlResourceParser parser = new AXmlResourceParser();
        parser.open(new ByteArrayInputStream(axml.getData()), axml.mTableStrings);
        boolean success = false;
        packageName = null;
        applicationName = null;

        int type;
        while ((type = parser.next()) != XmlPullParser.END_DOCUMENT) {
            if (type != XmlPullParser.START_TAG) continue;
            if (parser.getName().equals("manifest")) {
                int size = parser.getAttributeCount();
                for (int i = 0; i < size; ++i)
                    if (parser.getAttributeName(i).equals("package"))
                        packageName = parser.getAttributeValue(i);
            } else if (parser.getName().equals("application")) {
                int size = parser.getAttributeCount();
                for (int i = 0; i < size; ++i)
                    if (parser.getAttributeNameResource(i) == 0x01010003) {
                        isCustomizedApplication = true;
                        applicationName = parser.getAttributeValue(i);
                        int index = axml.mTableStrings.getSize();
                        byte[] data = axml.getData();
                        int off = parser.currentAttributeStart + 20 * i;
                        off += 8;
                        writeInt(data, off, index);
                        off += 8;
                        writeInt(data, off, index);
                    }
                if (!isCustomizedApplication) {
                    int off = parser.currentAttributeStart;
                    byte[] data = axml.getData();
                    byte[] newData = new byte[data.length + 20];
                    System.arraycopy(data, 0, newData, 0, off);
                    System.arraycopy(data, off, newData, off + 20, data.length - off);

                    // chunkSize
                    int chunkSize = readInt(newData, off - 32);
                    writeInt(newData, off - 32, chunkSize + 20);
                    // attributeCount
                    writeInt(newData, off - 8, size + 1);

                    int idIndex = parser.findResourceID(0x01010003);
                    if (idIndex == -1)
                        throw new IOException("idIndex == -1");

                    boolean isMax = true;
                    for (int i = 0; i < size; ++i) {
                        int id = parser.getAttributeNameResource(i);
                        if (id > 0x01010003) {
                            isMax = false;
                            if (i != 0) {
                                System.arraycopy(newData, off + 20, newData, off, 20 * i);
                                off += 20 * i;
                            }
                            break;
                        }
                    }
                    if (isMax) {
                        System.arraycopy(newData, off + 20, newData, off, 20 * size);
                        off += 20 * size;
                    }

                    writeInt(newData, off, axml.mTableStrings.find("http://schemas.android.com/apk/res/android"));
                    writeInt(newData, off + 4, idIndex);
                    writeInt(newData, off + 8, axml.mTableStrings.getSize());
                    writeInt(newData, off + 12, 0x03000008);
                    writeInt(newData, off + 16, axml.mTableStrings.getSize());
                    axml.setData(newData);
                }
                success = true;
                break;
            }
        }
        if (!success) throw new IOException();
        ArrayList<String> list = new ArrayList<>(axml.mTableStrings.getSize());
        axml.mTableStrings.getStrings(list);
        list.add(customApplicationName);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        axml.write(list, baos);
        isCustomizedApplication = false;
        return baos.toByteArray();
    }

    private static void writeInt(byte[] data, int off, int value) {
        data[off++] = (byte) (value & 0xFF);
        data[off++] = (byte) ((value >>> 8) & 0xFF);
        data[off++] = (byte) ((value >>> 16) & 0xFF);
        data[off] = (byte) ((value >>> 24) & 0xFF);
    }

    private static int readInt(byte[] data, int off) {
        return data[off + 3] << 24 | (data[off + 2] & 0xFF) << 16 | (data[off + 1] & 0xFF) << 8 | data[off] & 0xFF;
    }
}
