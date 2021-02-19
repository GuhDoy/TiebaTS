package bin.zip;

public interface CopyEntryCallback {
    /**
     * @param current 当前zipEntry序号
     * @param total   ZipEntry总个数
     * @return Null不进行复制 否则进行复制
     */
    ZipEntry filter(ZipEntry zipEntry, int current, int total);

    /**
     * @param current 当前已解压字节数
     * @param total   总字节数
     */
    void onProgress(long current, long total);

    void done(ZipEntry zipEntry);
}
