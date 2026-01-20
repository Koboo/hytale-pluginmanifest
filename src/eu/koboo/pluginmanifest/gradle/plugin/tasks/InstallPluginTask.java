package eu.koboo.pluginmanifest.gradle.plugin.tasks;

import eu.koboo.pluginmanifest.gradle.plugin.extension.serverruntime.ServerRuntimeExtension;
import eu.koboo.pluginmanifest.gradle.plugin.utils.FileUtils;
import eu.koboo.pluginmanifest.gradle.plugin.utils.PluginLog;
import lombok.extern.slf4j.Slf4j;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;

@Slf4j
public abstract class InstallPluginTask extends DefaultTask {

    @Input
    public abstract RegularFileProperty getArchiveFilePath();

    @Nested
    public abstract Property<ServerRuntimeExtension> getRuntimeExtension();

    @TaskAction
    public void runTask() {
        PluginLog.info("Installing plugin into server runtime..");
        ServerRuntimeExtension runtimeExt = getRuntimeExtension().get();
        File runtimeModDirectory = runtimeExt.resolveRuntimeModDirectory();
        if (!runtimeModDirectory.exists()) {
            throw new GradleException("Can't find mod directory in server: " + runtimeModDirectory.getAbsolutePath());
        }

        File archiveFile = getArchiveFilePath().get().getAsFile();
        if (!archiveFile.exists()) {
            throw new GradleException("Can't install plugin into server, because it doesn't exists: " + archiveFile.getAbsolutePath());
        }
        File runtimeArchiveFile = new File(runtimeModDirectory, archiveFile.getName());
        if (runtimeArchiveFile.exists()) {
            runtimeArchiveFile.delete();
            PluginLog.info("Deleted old plugin file: " + runtimeArchiveFile.getAbsolutePath());
        }

        try {
            FileUtils.copyFileTo(archiveFile, runtimeArchiveFile);
        } catch (IOException e) {
            throw new GradleException("Can't install plugin into server, because it didn't get copied: " + archiveFile.getAbsolutePath(), e);
        }

        PluginLog.info("");
        PluginLog.info("Successfully installed plugin into runtime!");
        PluginLog.info(" - from: " + archiveFile.getAbsolutePath());
        PluginLog.info(" -   to: " + runtimeArchiveFile.getAbsolutePath());
        PluginLog.info("");
    }
}
