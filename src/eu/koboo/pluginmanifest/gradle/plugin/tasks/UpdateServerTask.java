package eu.koboo.pluginmanifest.gradle.plugin.tasks;

import eu.koboo.pluginmanifest.gradle.plugin.PluginLog;
import eu.koboo.pluginmanifest.gradle.plugin.extension.serverdependency.ServerRuntimeExtension;
import eu.koboo.pluginmanifest.gradle.plugin.utils.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

@Slf4j
public abstract class UpdateServerTask extends DefaultTask {

    @Nested
    public abstract Property<ServerRuntimeExtension> getRuntimeExtension();

    @TaskAction
    public void runTask() {
        PluginLog.info("Updating server directory...");
        ServerRuntimeExtension runtimeExt = getRuntimeExtension().get();
        File runtimeDirectory = runtimeExt.resolveRuntimeDirectory();

        File clientServerJarFile = runtimeExt.resolveClientServerJarFile();
        File runtimeServerJarFile = runtimeExt.resolveRuntimeServerJarFile();
        runtimeServerJarFile.delete();
        PluginLog.info("Deleted server jar file " + runtimeServerJarFile.getAbsolutePath());
        FileUtils.copyClientFileToRuntime(clientServerJarFile, runtimeServerJarFile, "server jar file");

        File clientAOTFile = runtimeExt.resolveClientAOTFile();
        File runtimeAOTFile = runtimeExt.resolveRuntimeAOTFile();
        runtimeAOTFile.delete();
        PluginLog.info("Deleted server aot file " + runtimeAOTFile.getAbsolutePath());
        FileUtils.copyClientFileToRuntime(clientAOTFile, runtimeAOTFile, "server aot file");

        File clientAssetsFile = runtimeExt.resolveClientAssetsFile();
        File runtimeAssetsFile = runtimeExt.resolveRuntimeAssetsFile();
        runtimeAssetsFile.delete();
        PluginLog.info("Deleted assets file " + runtimeAssetsFile.getAbsolutePath());
        FileUtils.copyClientFileToRuntime(clientAssetsFile, runtimeAssetsFile, "assets zip file");

        PluginLog.info("");
        PluginLog.info("Successfully updated server directory:");
        PluginLog.info(runtimeDirectory.getAbsolutePath());
        PluginLog.info("");
    }
}
