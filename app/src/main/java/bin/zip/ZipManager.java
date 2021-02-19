package bin.zip;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class ZipManager {
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yy-MM-dd HH:mm");
    private final File file;
    public final ZipFile zipFile;
    private final ZipEntry[] ze;
    private ZipOutputStream zos;
    public File tmp;

//    public static void main(String[] args) {
//        test1();
//    }
//    private static void extractTest() {
//        try {
//            ZipManager zipManager = new ZipManager(new File("C:\\b.zip"));
//            zipManager.extract(new ExtractCallback() {
//
//                @Override
//                public File filter(ZipEntry zipEntry, int current, int total) {
//                    System.out.print("正在解压文件(" + current + "/" + total + ").." + zipEntry.getName() + "\n");
//                    File file = new File("C:\\b\\" + zipEntry.getName());
//                    if (!file.isDirectory() && file.exists())
//                        //noinspection ResultOfMethodCallIgnored
//                        file.delete();
//                    return file;
//                }
//
//                @Override
//                public void onProgress(long current, long total) {
//                    System.out.print((100 * current / total) + "%\n");
//                }
//
//                @Override
//                public void done(ZipEntry zipEntry, File file) {
//
//                }
//            });
//            System.out.print("done\n");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

//    private static void test2() {
//        try {
//            ZipManager zipManager = new ZipManager(new File("C:\\b.zip"));
//            for (ZipEntry ze : zipManager.ze) {
//                System.out.print(ze.getName() + " " + ze.getParent() + " " + ze.getSimpleName() + "\n");
//            }
//            System.out.print("===================\n");
//            ArrayList<ZipEntry> al = zipManager.list("新建文件夹/");
//            for (ZipEntry ze : al) {
//                System.out.print(ze.getName() + " " + getEntryTime(ze) + "\n");
//            }
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//    }

//    private static void test1() {
//        try {
//            ZipFile zipFile = new ZipFile(new File("D:\\a\\a.zip"));
//            System.out.print(zipFile.getEncoding() + "\n");
//            ZipOutputStream zos = new ZipOutputStream (new File("D:\\a\\b.zip"));
//            zos.setLevel(ZipOutputStream.LEVEL_BEST);
//            zos.setZipEncoding(zipFile.getZipEncoding());
//
//            Enumeration<ZipEntry> zes = zipFile.getEntries();
//            while(zes.hasMoreElements()){
//                ZipEntry zipEntry = zes.nextElement();
//                System.out.print(zipEntry.getName() + "\n");
//
//                if(zipEntry.getName().endsWith("/"))
//                    continue;
//
//                if (zipEntry.getName().equalsIgnoreCase("a.txt"))
//                    zipEntry.setName("我爱蛋蛋.txt");
//
//                zos.copyZipEntry(zipEntry, zipFile);
//            }
//            zos.putNextEntry("a.txt");
//            zos.writeFully(new FileInputStream(new File("D:\\a\\a.txt")));
//
//            zos.putNextEntry("文件夹/");
//
//            zipFile.close();
//            zos.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    public long getTotalCompressedSize() {
        long size = 0;
        for (ZipEntry zipEntry : ze) {
            if (!zipEntry.isDirectory())
                size += zipEntry.getCompressedSize();
        }
        return size;
    }

    public long getTotalSize() {
        long size = 0;
        for (ZipEntry zipEntry : ze) {
            if (!zipEntry.isDirectory())
                size += zipEntry.getSize();
        }
        return size;
    }

    public ZipEntry[] getZipEntries() {
        return ze;
    }

    public void zip(File file, long size, String entryName, ZipCallback callback) throws IOException {
        if (size > 0)
            callback.onProgress(0, size);
        InputStream is = new FileInputStream(file);
        ZipEntry zipEntry = new ZipEntry(entryName);
        zipEntry.setTime(file.lastModified());
        zos.putNextEntry(zipEntry);
        byte[] bytes = new byte[ZipOutputStream.BUFFER_SIZE];
        int len;
        long current = 0;
        while ((len = is.read(bytes)) > 0) {
            zos.write(bytes, 0, len);
            current += len;
            if (size > 0)
                callback.onProgress(current, size);
        }
        is.close();
        zos.closeEntry();
    }

    public HashMap<String, ZipEntry> getZipFileEntryMap() {
        HashMap<String, ZipEntry> map = new HashMap<>();
        for (ZipEntry zipEntry : ze) {
            if (!zipEntry.isDirectory())
                map.put(zipEntry.getName().toLowerCase(), zipEntry);
        }
        return map;
    }

    public void copyEntries(CopyEntryCallback callback) throws IOException {
        for (int i = 0; i < ze.length; i++) {
            ZipEntry zipEntry = callback.filter(ze[i], i + 1, ze.length);
            if (zipEntry == null)
                continue;
            if (zipEntry.isDirectory())
                zos.putNextEntry(zipEntry);
            else {
                InputStream rawInputStream = zipFile.getRawInputStream(zipEntry);
                zos.putNextRawEntry(zipEntry);
                int len;
                final long total = zipEntry.getCompressedSize();
                long current = 0;
                byte[] bytes = new byte[ZipOutputStream.BUFFER_SIZE];
                while ((len = rawInputStream.read(bytes)) > 0) {
                    zos.writeRaw(bytes, 0, len);
                    current += len;
                    callback.onProgress(current, total);
                }
            }
            // rawInputStream.close()是个空方法，是否调用不影响
            zos.closeEntry();
            callback.done(zipEntry);
        }
    }

    public void copyOtherZipManagerEntries(ZipManager zipManager, CopyEntryCallback callback) throws IOException {
        for (int i = 0; i < zipManager.ze.length; i++) {
            ZipEntry zipEntry = callback.filter(zipManager.ze[i], i + 1, zipManager.ze.length);
            if (zipEntry == null)
                continue;
            if (zipEntry.isDirectory())
                zos.putNextEntry(zipEntry);
            else {
                InputStream rawInputStream = zipManager.zipFile.getRawInputStream(zipManager.ze[i]);
                zos.putNextRawEntry(zipEntry);
                int len;
                final long total = zipEntry.getCompressedSize();
                long current = 0;
                byte[] bytes = new byte[ZipOutputStream.BUFFER_SIZE];
                while ((len = rawInputStream.read(bytes)) > 0) {
                    zos.writeRaw(bytes, 0, len);
                    current += len;
                    callback.onProgress(current, total);
                }
                // rawInputStream.close()是个空方法，是否调用不影响
            }
            zos.closeEntry();
            callback.done(zipEntry);
        }
    }

    public void extractZipEntry(ZipEntry zipEntry, File file) throws IOException {
        InputStream is = zipFile.getInputStream(zipEntry);
        BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(file));
        byte[] bytes = new byte[ZipOutputStream.BUFFER_SIZE];
        int len;
        while ((len = is.read(bytes)) > 0) {
            os.write(bytes, 0, len);
        }
        is.close();
        os.close();
    }

    public void extract(ExtractCallback callback) throws IOException {
        for (int i = 0; i < ze.length; i++) {
            ZipEntry zipEntry = ze[i];
            File file = callback.filter(zipEntry, i + 1, ze.length);
            if (file == null)
                continue;
            if (zipEntry.isDirectory()) {
                if (!file.exists() && !file.mkdirs())
                    throw new IOException("mkdir \"" + file.getPath() + "\" failed");
            } else {
                InputStream is = zipFile.getInputStream(zipEntry);
                BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(file));
                byte[] bytes = new byte[ZipOutputStream.BUFFER_SIZE];
                int len;
                long total = zipEntry.getSize();
                long current = 0;
                while ((len = is.read(bytes)) > 0) {
                    os.write(bytes, 0, len);
                    current += len;
                    if (total > 0)
                        callback.onProgress(current, total);
                }
                is.close();
                os.close();
            }
            //noinspection ResultOfMethodCallIgnored
            file.setLastModified(zipEntry.getTime());
            callback.done(zipEntry, file);
        }
    }

    public ZipOutputStream createTempZipOutputStream() throws IOException {
        String s = "";
        do {
            s += System.currentTimeMillis() % 1000000;
            tmp = new File(file.getParentFile(), s + ".tmp");
        } while (tmp.exists());
        zos = new ZipOutputStream(tmp);
        zos.setZipEncoding(zipFile.getZipEncoding());
        return zos;
    }

    public ZipManager(File file) throws IOException {
        zipFile = new ZipFile(this.file = file);
        ArrayList<ZipEntry> al = new ArrayList<>(zipFile.getEntrySize());
        Enumeration<ZipEntry> entryEnumeration = zipFile.getEntries();
        HashMap<String, ZipEntry> map = new HashMap<>();
        while (entryEnumeration.hasMoreElements()) {
            ZipEntry z = entryEnumeration.nextElement();
            if (!z.isDirectory())
                al.add(z);
            else {
                if (!map.containsKey(z.getName()))
                    map.put(z.getName(), z);
                else {
                    //该文件夹已处理过父目录，无需再处理
                    continue;
                }
            }
            String parent = z.getParent();
            if (parent != null) {
                long timeZ = z.getTime();
                if (!map.containsKey(parent)) {
                    // 父目录不存在，自动创建
                    do {
                        ZipEntry dir = new ZipEntry(parent);
                        dir.setTime(timeZ);
                        map.put(parent, dir);
                        parent = dir.getParent();
                    } while (parent != null && !map.containsKey(parent));
                } else {
                    // 父目录存在，更新日期
                    do {
                        ZipEntry dir = map.get(parent);
                        if (dir.getTime() < timeZ) {
                            dir.setTime(timeZ);
                            parent = dir.getParent();
                        } else
                            break;
                    } while (parent != null);
                }
            }
        }
        al.addAll(map.values());
        map.clear();
        Collections.sort(al, new Comparator<ZipEntry>() {
            @Override
            public int compare(ZipEntry o1, ZipEntry o2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });
        ze = new ZipEntry[al.size()];
        al.toArray(ze);
        al.clear();
    }

    public static String getEntryTime(ZipEntry zipEntry) {
        return DATE_FORMAT.format(zipEntry.getLastModifiedDate());
    }

    public int getEntrySize() {
        return ze.length;
    }

    public void close() throws IOException {
        zipFile.close();
    }

    public ArrayList<ZipEntry> list(String path) {
        final String p;
        if (path == null || path.length() <= 1)
            p = null;
        else if (path.charAt(path.length() - 1) != '/')
            p = path + "/";
        else
            p = path;
        final ArrayList<ZipEntry> l = new ArrayList<>(ze.length);
        for (ZipEntry zipEntry : ze) {
            String parent = zipEntry.getParent();
            if (parent == null) {
                if (p == null)
                    l.add(zipEntry);
            } else if (parent.equals(p))
                l.add(zipEntry);
        }
        l.trimToSize();
        return l;
    }
}
