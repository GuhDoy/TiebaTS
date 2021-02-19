package bin.util;

import bin.zip.ZipFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StreamUtil {

    public static byte[] readBytes(InputStream is) throws IOException {
        byte[] buf = new byte[10240];
        int num;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while ((num = is.read(buf)) != -1)
            baos.write(buf, 0, num);
        byte[] b = baos.toByteArray();
        baos.close();
        return b;
    }

    public static void close(InputStream is) {
        if (is != null)
            try {
                is.close();
            } catch (IOException ignored) {
            }
    }

    public static void close(OutputStream os) {
        if (os != null)
            try {
                os.close();
            } catch (IOException ignored) {
            }
    }

    public static void close(ZipFile zip) {
        if (zip != null)
            try {
                zip.close();
            } catch (IOException ignored) {
            }
    }

}
