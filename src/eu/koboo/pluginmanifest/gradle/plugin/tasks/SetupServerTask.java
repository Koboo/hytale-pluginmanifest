package eu.koboo.pluginmanifest.gradle.plugin.tasks;

import eu.koboo.pluginmanifest.gradle.plugin.extension.serverdependency.ServerRuntimeExtension;
import eu.koboo.pluginmanifest.gradle.plugin.PluginLog;
import eu.koboo.pluginmanifest.gradle.plugin.utils.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import java.io.File;

@Slf4j
public abstract class SetupServerTask extends DefaultTask {

    @Nested
    public abstract Property<ServerRuntimeExtension> getRuntimeExtension();

    @TaskAction
    public void runTask() {
        PluginLog.info("Setting up server directory...");
        ServerRuntimeExtension runtimeExt = getRuntimeExtension().get();
        File runtimeDirectory = runtimeExt.resolveRuntimeDirectory();

        File clientServerJarFile = runtimeExt.resolveClientServerJarFile();
        File runtimeServerJarFile = runtimeExt.resolveRuntimeServerJarFile();
        FileUtils.copyClientFileToRuntime(clientServerJarFile, runtimeServerJarFile, "server jar file");

        File clientAOTFile = runtimeExt.resolveClientAOTFile();
        File runtimeAOTFile = runtimeExt.resolveRuntimeAOTFile();
        FileUtils.copyClientFileToRuntime(clientAOTFile, runtimeAOTFile, "server aot file");

        File clientAssetsFile = runtimeExt.resolveClientAssetsFile();
        File runtimeAssetsFile = runtimeExt.resolveRuntimeAssetsFile();
        FileUtils.copyClientFileToRuntime(clientAssetsFile, runtimeAssetsFile, "assets zip file");

        PluginLog.info("");
        PluginLog.info("Successfully setup server directory!");
        PluginLog.info(runtimeDirectory.getAbsolutePath());
        PluginLog.info("");
    }
}
