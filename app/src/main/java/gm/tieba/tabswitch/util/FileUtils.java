package gm.tieba.tabswitch.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class FileUtils {
    public static void copy(final Object input, final Object output) throws IOException {
        final InputStream is;
        if (input instanceof InputStream) {
            is = (InputStream) input;
        } else if (input instanceof File) {
            is = new FileInputStream((File) input);
        } else if (input instanceof FileDescriptor) {
            is = new FileInputStream((FileDescriptor) input);
        } else if (input instanceof String) {
            is = new FileInputStream((String) input);
        } else throw new IllegalArgumentException("unknown input type");

        final OutputStream os;
        if (output instanceof OutputStream) {
            os = (OutputStream) output;
        } else if (output instanceof File) {
            os = new FileOutputStream((File) output);
        } else if (output instanceof FileDescriptor) {
            os = new FileOutputStream((FileDescriptor) output);
        } else if (output instanceof String) {
            os = new FileOutputStream((String) output);
        } else throw new IllegalArgumentException("unknown output type");

        copy(is, os);
    }

    private static void copy(final InputStream is, final OutputStream os) throws IOException {
        final byte[] buffer = new byte[8192];
        int byteCount;
        while ((byteCount = is.read(buffer)) != -1) {
            os.write(buffer, 0, byteCount);
        }
        os.flush();
        is.close();
        os.close();
    }

    public static void copy(final ByteBuffer bb, final Object output) throws IOException {
        final OutputStream os;
        if (output instanceof OutputStream) {
            os = (OutputStream) output;
        } else if (output instanceof File) {
            os = new FileOutputStream((File) output);
        } else if (output instanceof FileDescriptor) {
            os = new FileOutputStream((FileDescriptor) output);
        } else if (output instanceof String) {
            os = new FileOutputStream((String) output);
        } else throw new IllegalArgumentException("unknown output type");

        os.write(bb.array());
    }

    public static ByteBuffer toByteBuffer(final InputStream is) throws IOException {
        final var baos = new ByteArrayOutputStream();
        copy(is, baos);
        return ByteBuffer.wrap(baos.toByteArray());
    }

    public static String getExtension(final ByteBuffer bb) throws IOException {
        final var chunk = new String(bb.array(), 0, 6);
        try {
            if (chunk.contains("GIF")) return "gif";
            else if (chunk.contains("PNG")) return "png";
            else return "jpeg";
        } finally {
            bb.rewind();
        }
    }

    public static String getParent(final String path) {
        final int index = path.lastIndexOf(File.separatorChar);
        return path.substring(0, index);
    }

    public static void deleteRecursively(final File file) {
        if (file.isDirectory()) {
            final File[] files = file.listFiles();
            if (files != null) {
                for (final File f : files) {
                    deleteRecursively(f);
                }
            }
        }
        file.delete();
    }
}
