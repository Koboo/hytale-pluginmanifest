package eu.koboo.pluginmanifest.gradle.plugin.utils;

import lombok.experimental.UtilityClass;
import org.gradle.api.GradleException;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

@UtilityClass
public class FileUtils {

    public void copyClientFileToRuntime(File clientFile, File runtimeFile, String type) {
        PluginLog.info("Copying client's " + type + " to runtime directory...");
        if (runtimeFile.exists()) {
            PluginLog.info("Runtime's " + type + " already exists!");
            return;
        }
        if (!clientFile.exists()) {
            throw new GradleException("Can't setup server, client's " + type + " doesn't exists: " + clientFile.getAbsolutePath());
        }
        PluginLog.info(" - client: " + clientFile.getAbsolutePath());
        PluginLog.info(" - runtime: " + runtimeFile.getAbsolutePath());
        try {
            copyFileTo(clientFile, runtimeFile);
        } catch (IOException e) {
            throw new GradleException("Can't setup server, couldn't copy client's " + type + ":" + clientFile.getAbsolutePath(), e);
        }
    }

    public void copyFileTo(File sourceFile, File destFile) throws IOException {
        Files.copy(
            sourceFile.toPath(),
            destFile.toPath(),
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.COPY_ATTRIBUTES
        );
    }

    public void deleteRecursively(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }

        Files.walkFileTree(dir, new SimpleFileVisitor<>() {
            @Override
            public @NonNull FileVisitResult visitFile(@NonNull Path file, @NonNull BasicFileAttributes attrs)
                throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public @NonNull FileVisitResult postVisitDirectory(@NonNull Path dir, IOException exc)
                throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
