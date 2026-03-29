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
    private static Logger LOGGER;
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
    private String addPrefix(String path) {
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
        //@Nullable JRZip zipfile = sharedZipFileAccess.getZipFile();
        if (zipfile == null) {
            return null;
        } else {
            ZipArchiveEntry zipentry = zipfile.getEntry(this.addPrefix(resourcePath));
            //JRZip.JrZipEntry zipentry = zipfile.getEntry(this.addPrefix(resourcePath));
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

        //@Nullable JRZip zipfile = sharedZipFileAccess.getZipFile();
        if (zipfile == null) {
            cir.setReturnValue(Set.of());
        } else {
            Enumeration<ZipArchiveEntry> entries = zipfile.getEntries();

            //Enumeration<? extends JRZip.JrZipEntry> entries = zipfile.entries();
            Set<String> namespaces = Sets.newHashSet();
            String typePrefix = this.addPrefix(type.getDirectory() + "/");

            while (entries.hasMoreElements()) {
                ZipArchiveEntry zipEntry = entries.nextElement();
                //JRZip.JrZipEntry zipEntry = entries.nextElement();
                MoreFormatAPI.getCompressed(sharedZipFileAccess, zipEntry);
                String name = zipEntry.getName();
                //String name = zipEntry.name();
                String namespace = FilePackResources.extractNamespace(typePrefix, name);
                if (!namespace.isEmpty()) {
                    if (Identifier.isValidNamespace(namespace)) {
                        namespaces.add(namespace);
                    } else {
                        LOGGER.warn("Non {} character in namespace {} in pack {}, ignoring", "[a-z0-9_.-]", namespace, this.sharedZipFileAccess.file);
                    }
                }
            }

            cir.setReturnValue(namespaces);
        }
    }

    @Inject(method = "listResources", at = @At("HEAD"), cancellable = true)
    private void pchf$listResources(PackType type, String namespace, String directory, PackResources.ResourceOutput output, CallbackInfo ci) {
        @Nullable ZipFile zipFile = sharedZipFileAccess.getZipFile();
        //@Nullable JRZip zipFile = sharedZipFileAccess.getZipFile();
        if (zipFile != null) {
            Enumeration<ZipArchiveEntry> enumeration = zipFile.getEntries();
            //Enumeration<? extends JRZip.JrZipEntry> enumeration = zipFile.entries();
            String var10001 = type.getDirectory();
            String root = this.addPrefix(var10001 + "/" + namespace + "/");
            String prefix = root + directory + "/";

            while (enumeration.hasMoreElements()) {
                ZipArchiveEntry zipEntry = enumeration.nextElement();
                //JRZip.JrZipEntry zipEntry = enumeration.nextElement();
                if (!zipEntry.isDirectory()) {
                    MoreFormatAPI.getCompressed(sharedZipFileAccess, zipEntry);
                    String name = zipEntry.getName();
                    //String name = zipEntry.name();
                    if (name.startsWith(prefix)) {
                        String path = name.substring(root.length());
                        Identifier id = Identifier.tryBuild(namespace, path);
                        if (id != null) {
                            output.accept(id, () -> zipFile.getInputStream(zipEntry));
                        } else {
                            LOGGER.warn("Invalid path in datapack: {}:{}, ignoring", namespace, path);
                        }
                    }
                }
            }

        }
        ci.cancel();
    }
}
