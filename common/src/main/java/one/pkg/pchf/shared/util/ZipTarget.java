package one.pkg.pchf.shared.util;

public class ZipTarget {
    public static final int ZSTD_METHOD = 93;
    public static final int BROTLI_METHOD = 100;

    public static boolean isZst(String filename) {
        return isSupported(filename) == 1;
    }

    public static boolean isBrotli(String filename) {
        return isSupported(filename) == 2;
    }

    public static int isSupported(String filename) {
        if (filename == null) return 0;
        String lower = filename.toLowerCase();
        if (lower.endsWith(".zst")) {
            return 1;
        } else if (lower.endsWith(".br")) {
            return 2;
        } else if (lower.endsWith(".zip")) {
            return 3;
        }
        return 0;
    }
}
