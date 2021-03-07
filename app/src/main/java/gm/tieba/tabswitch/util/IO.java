package gm.tieba.tabswitch.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class IO {
    private static final int BUFFER_SIZE = 10240;

    public static void copyFile(Object input, Object output) throws IOException {
        InputStream inputStream;
        if (input instanceof InputStream)
            inputStream = (InputStream) input;
        else if (input instanceof File)
            inputStream = new FileInputStream((File) input);
        else if (input instanceof FileDescriptor)
            inputStream = new FileInputStream((FileDescriptor) input);
        else if (input instanceof String)
            inputStream = new FileInputStream((String) input);
        else throw new IOException("unknown input type");

        FileOutputStream fileOutputStream;
        if (input instanceof FileOutputStream)
            fileOutputStream = (FileOutputStream) input;
        else if (output instanceof File)
            fileOutputStream = new FileOutputStream((File) output);
        else if (output instanceof FileDescriptor)
            fileOutputStream = new FileOutputStream((FileDescriptor) output);
        else if (output instanceof String)
            fileOutputStream = new FileOutputStream((String) output);
        else throw new IOException("unknown output type");

        copyFile(inputStream, fileOutputStream);
    }

    private static void copyFile(InputStream is, FileOutputStream fos) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int byteCount;
        while ((byteCount = is.read(buffer)) != -1)
            fos.write(buffer, 0, byteCount);
        fos.flush();
        is.close();
        fos.close();
    }

    public static ByteArrayOutputStream cloneInputStream(InputStream input) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[BUFFER_SIZE];
        int len;
        while ((len = input.read(buffer)) > -1)
            baos.write(buffer, 0, len);
        baos.flush();
        return baos;
    }

    public static String getExtension(InputStream inputStream) throws IOException {
        byte[] buffer = new byte[10];
        if (inputStream.read(buffer) == -1) throw new IOException();
        if (Arrays.equals(buffer, new byte[]{-1, -40, -1, -32, 0, 16, 74, 70, 73, 70}))
            return "jpg";
        else if (Arrays.equals(buffer, new byte[]{-119, 80, 78, 71, 13, 10, 26, 10, 0, 0}))
            return "png";
        else return "gif";
    }

    public static void deleteFiles(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null && files.length > 0)
                for (File f : files)
                    deleteFiles(f);
        }
        file.delete();
    }
}