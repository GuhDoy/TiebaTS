package gm.tieba.tabswitch.util;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class IO {
    public static void copyFileFromAssets(Context context, String assetsFilePath, String targetFileFullPath) throws IOException {
        InputStream assetsFileInputStream = context.getAssets().open(assetsFilePath);
        copyFileFromStream(assetsFileInputStream, targetFileFullPath);
    }

    public static void copyFileFromStream(InputStream in, String targetPath) throws IOException {
        FileOutputStream fos = new FileOutputStream(new File(targetPath));
        byte[] buffer = new byte[8192];
        int byteCount;
        while ((byteCount = in.read(buffer)) != -1)
            fos.write(buffer, 0, byteCount);
        fos.flush();
        in.close();
        fos.close();
    }

    public static void copyFile(InputStream in, FileOutputStream fos) throws IOException {
        byte[] buffer = new byte[8192];
        int byteCount;
        while ((byteCount = in.read(buffer)) != -1)
            fos.write(buffer, 0, byteCount);
        fos.flush();
        in.close();
        fos.close();
    }

    public static ByteArrayOutputStream cloneInputStream(InputStream input) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = input.read(buffer)) > -1)
            baos.write(buffer, 0, len);
        baos.flush();
        return baos;
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