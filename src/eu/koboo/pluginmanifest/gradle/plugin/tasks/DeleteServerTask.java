package eu.koboo.pluginmanifest.gradle.plugin.tasks;

import eu.koboo.pluginmanifest.gradle.plugin.utils.PluginLog;
import eu.koboo.pluginmanifest.gradle.plugin.extension.serverdependency.ServerRuntimeExtension;
import lombok.extern.slf4j.Slf4j;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

@Slf4j
public abstract class DeleteServerTask extends DefaultTask {

    @Nested
    public abstract Property<ServerRuntimeExtension> getRuntimeExtension();

    @TaskAction
    public void runTask() throws IOException {
        PluginLog.info("Deleting server directory...");
        ServerRuntimeExtension runtimeExt = getRuntimeExtension().get();
        File runtimeDirectory = runtimeExt.resolveRuntimeDirectory();
        // Walk the file tree and delete everything
        Files.walkFileTree(runtimeDirectory.toPath(), new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path directory, IOException exc) throws IOException {
                Files.delete(directory); // delete directory after contents
                return FileVisitResult.CONTINUE;
            }
        });

        PluginLog.info("");
        PluginLog.info("Successfully delete server directory:");
        PluginLog.info(runtimeDirectory.getAbsolutePath());
        PluginLog.info("");
    }
}
