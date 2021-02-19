package bin.zip.encoding;

public class ZipEncodingHelper {
    public static final ZipEncoding UTF8_ZIP_ENCODING = new FallbackZipEncoding("UTF-8");

    /**
     * name of the encoding UTF-8
     */
    static final String UTF8 = "UTF8";

    /**
     * variant name of the encoding UTF-8 used for comparisons.
     */
    private static final String UTF_DASH_8 = "utf-8";

    /**
     * Whether a given encoding - or the platform's default encoding
     * if the parameter is null - is UTF-8.
     */
    public static boolean isUTF8(String encoding) {
        if (encoding == null) {
            // check platform's default encoding
            encoding = System.getProperty("file.encoding");
        }
        return UTF8.equalsIgnoreCase(encoding)
                || UTF_DASH_8.equalsIgnoreCase(encoding);
    }

    public static ZipEncoding getZipEncoding(String encoding) {
        return new FallbackZipEncoding(encoding);
    }
}
