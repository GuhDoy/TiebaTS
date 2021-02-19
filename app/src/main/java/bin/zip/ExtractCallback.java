package bin.zip;

import java.io.File;

public interface ExtractCallback {
    /**
     * @param current  当前Entry序号
     * @param total    总Entry个数
     * @return Null不进行解压 否则进行解压
     */
    File filter(ZipEntry zipEntry, int current, int total);

    /**
     * @param current 当前文件已解压字节数
     * @param total   当前文件总字节数
     */
    void onProgress(long current, long total);

    void done(ZipEntry zipEntry, File file);
}
