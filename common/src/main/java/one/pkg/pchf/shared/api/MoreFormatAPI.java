package one.pkg.pchf.shared.api;

//import one.pkg.jrzip.JRZip;

import one.pkg.pchf.shared.util.SharedZipFileAccess;
import one.pkg.pchf.shared.util.ZipTarget;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;

import java.util.zip.ZipEntry;

public class MoreFormatAPI {
    public static boolean isAPIEnabled() {
        return false;
    }

    //public static void getCompressed(SharedZipFileAccess access, JRZip.JrZipEntry zipEntry) {
        /*if (!isAPIEnabled()) return;
        if (zipEntry.method() == ZipEntry.DEFLATED) {
            if (access.isZstd()) zipEntry.setMethod(ZipTarget.ZSTD_METHOD);
            else if (access.isBrotli()) zipEntry.setMethod(ZipTarget.BROTLI_METHOD);
        }*/
    //}

    public static void getCompressed(SharedZipFileAccess access, ZipArchiveEntry zipEntry) {
        if (!isAPIEnabled()) return;
        if (zipEntry.getMethod() == ZipEntry.DEFLATED) {
            if (access.isZstd()) zipEntry.setMethod(ZipTarget.ZSTD_METHOD);
            else if (access.isBrotli()) zipEntry.setMethod(ZipTarget.BROTLI_METHOD);
        }
    }
}
