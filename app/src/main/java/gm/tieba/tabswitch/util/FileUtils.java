package gm.tieba.tabswitch.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class FileUtils {
    public static void copy(Object input, Object output) throws IOException {
        InputStream is;
        if (input instanceof InputStream) {
            is = (InputStream) input;
        } else if (input instanceof File) {
            is = new FileInputStream((File) input);
        } else if (input instanceof FileDescriptor) {
            is = new FileInputStream((FileDescriptor) input);
        } else if (input instanceof String) {
            is = new FileInputStream((String) input);
        } else throw new IOException("unknown input type");

        OutputStream os;
        if (output instanceof OutputStream) {
            os = (OutputStream) output;
        } else if (output instanceof File) {
            os = new FileOutputStream((File) output);
        } else if (output instanceof FileDescriptor) {
            os = new FileOutputStream((FileDescriptor) output);
        } else if (output instanceof String) {
            os = new FileOutputStream((String) output);
        } else throw new IOException("unknown output type");

        copy(is, os);
    }

    private static void copy(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[8192];
        int byteCount;
        while ((byteCount = is.read(buffer)) != -1) {
            os.write(buffer, 0, byteCount);
        }
        os.flush();
        is.close();
        os.close();
    }

    public static ByteArrayOutputStream cloneInputStream(InputStream in) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        copy(in, baos);
        return baos;
    }

    // TODO: use ByteBuffer
    public static String getExtension(ByteArrayOutputStream baos) throws IOException {
        InputStream inputStream = new ByteArrayInputStream(baos.toByteArray());
        byte[] bytes = new byte[6];
        if (inputStream.read(bytes) == -1) throw new IOException();
        String chunk = new String(bytes, StandardCharsets.UTF_8);
        if (chunk.contains("GIF")) return "gif";
        if (chunk.contains("PNG")) return "png";
        return "jpg";
    }

    public static String getParent(String path) {
        int index = path.lastIndexOf(File.separatorChar);
        return path.substring(0, index);
    }

    public static void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    deleteRecursively(f);
                }
            }
        }
        file.delete();
    }
}
