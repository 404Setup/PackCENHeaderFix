package one.pkg.pchf.shared.util;

import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class SharedZipFileAccess implements AutoCloseable {
    public static final Logger LOGGER = LoggerFactory.getLogger("SharedZipFileAccess");
    public final File file;
    private final boolean zstd;
    private final boolean brotli;
    private boolean failedToLoad;
    private ZipFile zipFile;

    protected SharedZipFileAccess(File file) {
        this.file = file;
        int supported = ZipTarget.isSupported(file.getName());
        this.zstd = supported == 1;
        this.brotli = supported == 2;
    }

    public static SharedZipFileAccess access(File file) {
        return new SharedZipFileAccess(file);
    }

    @Nullable
    public ZipFile getZipFile() {
        if (this.failedToLoad) {
            return null;
        } else {
            if (this.zipFile == null) {
                try {
                    this.zipFile = ZipFile.builder().setFile(this.file).get();
                } catch (IOException iOException) {
                    LOGGER.error("Failed to open pack {}", this.file, iOException);
                    this.failedToLoad = true;
                    return null;
                }
            }

            return this.zipFile;
        }
    }

    @Override
    public void close() {
        if (this.zipFile != null) {
            IOUtils.closeQuietly(this.zipFile);
            this.zipFile = null;
        }
    }

    public boolean isBrotli() {
        return brotli;
    }

    public boolean isZstd() {
        return zstd;
    }
}
