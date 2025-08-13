package one.pkg.pchf.shared.util;

import net.minecraft.server.packs.FilePackResources;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class SharedZipFileAccess extends FilePackResources.SharedZipFileAccess {
    public static final Logger LOGGER = LoggerFactory.getLogger("SharedZipFileAccess");
    private final boolean zstd;
    private final boolean brotli;
    private ZipFile vzipFile;

    protected SharedZipFileAccess(File file) {
        super(file);
        int supported = ZipTarget.isSupported(file.getName());
        this.zstd = supported == 1;
        this.brotli = supported == 2;
    }

    public static SharedZipFileAccess access(File file) {
        return new SharedZipFileAccess(file);
    }

    @Nullable
    public ZipFile getACZipFile() {
        if (this.failedToLoad) {
            return null;
        } else {
            if (this.vzipFile == null) {
                try {
                    this.vzipFile = ZipFile.builder().setFile(this.file).get();
                } catch (IOException iOException) {
                    LOGGER.error("Failed to open pack {}", this.file, iOException);
                    this.failedToLoad = true;
                    return null;
                }
            }

            return this.vzipFile;
        }
    }

    @Override
    public void close() {
        if (this.vzipFile != null) {
            IOUtils.closeQuietly(this.vzipFile);
            this.vzipFile = null;
        }
        super.close();
    }

    public boolean isBrotli() {
        return brotli;
    }

    public boolean isZstd() {
        return zstd;
    }
}
