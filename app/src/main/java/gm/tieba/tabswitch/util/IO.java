package gm.tieba.tabswitch.util;

import android.os.FileUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class IO {
    public static void copyFile(Object input, Object output) throws IOException {
        InputStream inputStream;
        if (input instanceof InputStream) {
            inputStream = (InputStream) input;
        } else if (input instanceof File) {
            inputStream = new FileInputStream((File) input);
        } else if (input instanceof FileDescriptor) {
            inputStream = new FileInputStream((FileDescriptor) input);
        } else if (input instanceof String) {
            inputStream = new FileInputStream((String) input);
        } else throw new IOException("unknown input type");

        FileOutputStream fileOutputStream;
        if (output instanceof FileOutputStream) {
            fileOutputStream = (FileOutputStream) output;
        } else if (output instanceof File) {
            fileOutputStream = new FileOutputStream((File) output);
        } else if (output instanceof FileDescriptor) {
            fileOutputStream = new FileOutputStream((FileDescriptor) output);
        } else if (output instanceof String) {
            fileOutputStream = new FileOutputStream((String) output);
        } else throw new IOException("unknown output type");

        FileUtils.copy(inputStream, fileOutputStream);
    }

    public static ByteArrayOutputStream cloneInputStream(InputStream in) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FileUtils.copy(in, baos);
        return baos;
    }

    public static String getExtension(ByteArrayOutputStream baos) throws IOException {
        InputStream inputStream = new ByteArrayInputStream(baos.toByteArray());
        byte[] bytes = new byte[6];
        if (inputStream.read(bytes) == -1) throw new IOException();
        String chunk = new String(bytes, StandardCharsets.UTF_8);
        if (chunk.contains("GIF")) return "gif";
        if (chunk.contains("PNG")) return "png";
        return "jpg";
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