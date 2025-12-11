package one.pkg.pchf.shared.mixin;

import com.google.common.collect.Sets;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.resources.IoSupplier;
import one.pkg.pchf.shared.api.MoreFormatAPI;
import one.pkg.pchf.shared.util.SharedZipFileAccess;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.InputStream;
import java.util.Enumeration;
import java.util.Set;

@Mixin(FilePackResources.class)
public abstract class FilePackResourcesMixin extends AbstractPackResources {
    @Final
    @Shadow
    static Logger LOGGER;
    @Unique
    private SharedZipFileAccess sharedZipFileAccess;

    protected FilePackResourcesMixin(PackLocationInfo packLocationInfo) {
        super(packLocationInfo);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void setInit(PackLocationInfo location, FilePackResources.SharedZipFileAccess zipFileAccess, String prefix, CallbackInfo ci) {
        sharedZipFileAccess = SharedZipFileAccess.access(zipFileAccess.file);
        zipFileAccess.close();
    }

    @Shadow
    private String addPrefix(String string) {
        return null;
    }

    /**
     * @author 404
     * @reason Replaced with Apache common compression. Compatible with MorePackFormat.
     */
    @Nullable
    @Overwrite
    private IoSupplier<InputStream> getResource(String resourcePath) {
        ZipFile zipfile = sharedZipFileAccess.getZipFile();
        if (zipfile == null) {
            return null;
        } else {
            ZipArchiveEntry zipentry = zipfile.getEntry(this.addPrefix(resourcePath));
            if (zipentry == null) return null;
            MoreFormatAPI.getCompressed(sharedZipFileAccess, zipentry);
            return () -> zipfile.getInputStream(zipentry);
        }
    }

    @Inject(method = "close", at = @At("RETURN"))
    private void pchf$close(CallbackInfo ci) {
        sharedZipFileAccess.close();
    }

    @Inject(method = "getNamespaces", at = @At("HEAD"), cancellable = true)
    private void pchf$getNamespaces(PackType type, CallbackInfoReturnable<Set<String>> cir) {
        @Nullable ZipFile zipfile = sharedZipFileAccess.getZipFile();
        if (zipfile == null) {
            cir.setReturnValue(Set.of());
        } else {
            Enumeration<ZipArchiveEntry> enumeration = zipfile.getEntries();
            Set<String> set = Sets.newHashSet();
            String s = this.addPrefix(type.getDirectory() + "/");

            while (enumeration.hasMoreElements()) {
                ZipArchiveEntry zipentry = enumeration.nextElement();
                MoreFormatAPI.getCompressed(sharedZipFileAccess, zipentry);
                String s1 = zipentry.getName();
                String s2 = FilePackResources.extractNamespace(s, s1);
                if (!s2.isEmpty()) {
                    if (Identifier.isValidNamespace(s2)) {
                        set.add(s2);
                    } else {
                        LOGGER.warn("Non [a-z0-9_.-] character in namespace {} in pack {}, ignoring",
                                s2,
                                this.sharedZipFileAccess.file
                        );
                    }
                }
            }

            cir.setReturnValue(set);
        }
    }

    @Inject(method = "listResources", at = @At("HEAD"), cancellable = true)
    private void pchf$listResources(PackType packType, String string, String string2, PackResources.ResourceOutput resourceOutput, CallbackInfo ci) {
        @Nullable ZipFile zipFile = sharedZipFileAccess.getZipFile();
        if (zipFile != null) {
            Enumeration<ZipArchiveEntry> enumeration = zipFile.getEntries();
            String var10001 = packType.getDirectory();
            String string3 = this.addPrefix(var10001 + "/" + string + "/");
            String string4 = string3 + string2 + "/";

            while (enumeration.hasMoreElements()) {
                ZipArchiveEntry zipEntry = enumeration.nextElement();
                if (!zipEntry.isDirectory()) {
                    MoreFormatAPI.getCompressed(sharedZipFileAccess, zipEntry);
                    String string5 = zipEntry.getName();
                    if (string5.startsWith(string4)) {
                        String string6 = string5.substring(string3.length());
                        Identifier identifier = Identifier.tryBuild(string, string6);
                        if (identifier != null) {
                            resourceOutput.accept(identifier, () -> zipFile.getInputStream(zipEntry));
                        } else {
                            LOGGER.warn("Invalid path in datapack: {}:{}, ignoring", string, string6);
                        }
                    }
                }
            }

        }
        ci.cancel();
    }
}
