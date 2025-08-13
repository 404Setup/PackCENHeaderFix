package one.pkg.pchf.shared.mixin;

import com.google.common.collect.Sets;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import one.pkg.pchf.shared.api.MoreFormatAPI;
import one.pkg.pchf.shared.api.VanillaRPAPI;
import one.pkg.pchf.shared.util.SharedZipFileAccess;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.InputStream;
import java.util.Enumeration;
import java.util.Set;

@Mixin(FilePackResources.class)
public abstract class FilePackResourcesMixin extends AbstractPackResources implements VanillaRPAPI {
    @Final
    @Shadow
    static Logger LOGGER;
    @Final
    @Shadow
    private FilePackResources.SharedZipFileAccess zipFileAccess;

    protected FilePackResourcesMixin(PackLocationInfo packLocationInfo) {
        super(packLocationInfo);
    }

    @ModifyVariable(
            method = "<init>",
            at = @At("HEAD"),
            ordinal = 0,
            argsOnly = true
    )
    private static FilePackResources.SharedZipFileAccess modifyZipFileAccess(FilePackResources.SharedZipFileAccess original) {
        if (original instanceof SharedZipFileAccess) return original;

        return SharedZipFileAccess.access(original.file);
    }

    @Shadow
    private String addPrefix(String string) {
        return null;
    }

    @Override
    public SharedZipFileAccess getSharedZipFileAccess() {
        return (SharedZipFileAccess) this.zipFileAccess;
    }

    /**
     * @author 404
     * @reason Replaced with Apache common compression. Compatible with MorePackFormat.
     */
    @Overwrite
    private IoSupplier<InputStream> getResource(String resourcePath) {
        ZipFile zipfile = getSharedZipFileAccess().getACZipFile();
        if (zipfile == null) {
            return null;
        } else {
            ZipArchiveEntry zipentry = zipfile.getEntry(this.addPrefix(resourcePath));
            if (zipentry == null) return null;
            MoreFormatAPI.getCompressed(getSharedZipFileAccess(), zipentry);
            return () -> zipfile.getInputStream(zipentry);
        }
    }

    @Inject(method = "getNamespaces", at = @At("HEAD"), cancellable = true)
    private void vrkmod$getNamespaces(PackType type, CallbackInfoReturnable<Set<String>> cir) {
        @Nullable ZipFile zipfile = getSharedZipFileAccess().getACZipFile();
        if (zipfile == null) {
            cir.setReturnValue(Set.of());
        } else {
            Enumeration<ZipArchiveEntry> enumeration = zipfile.getEntries();
            Set<String> set = Sets.newHashSet();
            String s = this.addPrefix(type.getDirectory() + "/");

            while (enumeration.hasMoreElements()) {
                ZipArchiveEntry zipentry = enumeration.nextElement();
                MoreFormatAPI.getCompressed(getSharedZipFileAccess(), zipentry);
                String s1 = zipentry.getName();
                String s2 = FilePackResources.extractNamespace(s, s1);
                if (!s2.isEmpty()) {
                    if (ResourceLocation.isValidNamespace(s2)) {
                        set.add(s2);
                    } else {
                        LOGGER.warn("Non [a-z0-9_.-] character in namespace {} in pack {}, ignoring",
                                s2,
                                this.zipFileAccess.file
                        );
                    }
                }
            }

            cir.setReturnValue(set);
        }
    }

    @Inject(method = "listResources", at = @At("HEAD"), cancellable = true)
    private void vrkmod$listResources(PackType packType, String string, String string2, PackResources.ResourceOutput resourceOutput, CallbackInfo ci) {
        @Nullable ZipFile zipFile = getSharedZipFileAccess().getACZipFile();
        if (zipFile != null) {
            Enumeration<ZipArchiveEntry> enumeration = zipFile.getEntries();
            String var10001 = packType.getDirectory();
            String string3 = this.addPrefix(var10001 + "/" + string + "/");
            String string4 = string3 + string2 + "/";

            while (enumeration.hasMoreElements()) {
                ZipArchiveEntry zipEntry = enumeration.nextElement();
                if (!zipEntry.isDirectory()) {
                    MoreFormatAPI.getCompressed(getSharedZipFileAccess(), zipEntry);
                    String string5 = zipEntry.getName();
                    if (string5.startsWith(string4)) {
                        String string6 = string5.substring(string3.length());
                        ResourceLocation resourceLocation = ResourceLocation.tryBuild(string, string6);
                        if (resourceLocation != null) {
                            resourceOutput.accept(resourceLocation, () -> zipFile.getInputStream(zipEntry));
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
