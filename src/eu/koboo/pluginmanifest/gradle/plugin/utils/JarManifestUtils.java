package eu.koboo.pluginmanifest.gradle.plugin.utils;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

@UtilityClass
public class JarManifestUtils {
    private static final String VERSION_UNKNOWN = "Unknown";
    private static final String VERSION_KEY = "Implementation-Version";

    public @Nullable Manifest getManifest(@Nullable File file) {
        if (file == null || !file.isFile() || file.isDirectory() || !file.exists()) {
            return null;
        }
        Manifest manifest;
        try {
            JarFile jarFile = new JarFile(file);
            manifest = jarFile.getManifest();
            jarFile.close();
        } catch (IOException e) {
            return null;
        }
        return manifest;
    }

    public @NotNull String getVersion(@Nullable File file) {
        Manifest manifest = getManifest(file);
        return getVersion(manifest);
    }

    public @NotNull String getVersion(@Nullable Manifest manifest) {
        if (manifest == null) {
            return VERSION_UNKNOWN;
        }
        String version = manifest.getMainAttributes().getValue(VERSION_KEY);
        if (version == null || version.trim().isEmpty()) {
            return VERSION_UNKNOWN;
        }
        return version;
    }

    public boolean isUnknown(String version) {
        return version == null || version.trim().isEmpty() || version.equals(VERSION_UNKNOWN);
    }
}
