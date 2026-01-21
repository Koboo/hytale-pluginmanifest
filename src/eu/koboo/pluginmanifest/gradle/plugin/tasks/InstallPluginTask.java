package eu.koboo.pluginmanifest.gradle.plugin.tasks;

import eu.koboo.pluginmanifest.gradle.plugin.utils.FileUtils;
import eu.koboo.pluginmanifest.gradle.plugin.utils.PluginLog;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.*;
import org.gradle.work.DisableCachingByDefault;

import java.io.File;
import java.io.IOException;

@DisableCachingByDefault(because = "Builds and copies the plugin into the configured hytale server")
public abstract class InstallPluginTask extends DefaultTask {

    @InputFile
    public abstract RegularFileProperty getArchiveFilePath();

    @InputDirectory
    public abstract DirectoryProperty getRuntimeModDirectory();

    @TaskAction
    public void runTask() {
        PluginLog.info("Installing plugin into server runtime..");
        File runtimeModDirectory = getRuntimeModDirectory().getAsFile().get();
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
