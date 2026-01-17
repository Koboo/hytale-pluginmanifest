package eu.koboo.pluginmanifest.gradle.plugin.tasks;

import eu.koboo.pluginmanifest.gradle.plugin.extension.clientinstall.ClientInstallationExtension;
import eu.koboo.pluginmanifest.gradle.plugin.extension.serverdependency.ServerRuntimeExtension;
import eu.koboo.pluginmanifest.gradle.plugin.utils.FileUtils;
import eu.koboo.pluginmanifest.gradle.plugin.utils.PluginLog;
import lombok.extern.slf4j.Slf4j;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.TaskAction;

import java.io.File;

@Slf4j
public abstract class SetupServerTask extends DefaultTask {

    @Nested
    public abstract Property<ServerRuntimeExtension> getRuntimeExtension();

    @Nested
    public abstract Property<ClientInstallationExtension> getInstallationExtension();

    @TaskAction
    public void runTask() {
        PluginLog.info("Setting up server directory...");
        ServerRuntimeExtension runtimeExt = getRuntimeExtension().get();
        ClientInstallationExtension installExt = getInstallationExtension().get();

        File runtimeDirectory = runtimeExt.resolveRuntimeDirectory();

        File clientServerJarFile = installExt.resolveClientServerJarFile();
        File runtimeServerJarFile = runtimeExt.resolveRuntimeServerJarFile();
        FileUtils.copyClientFileToRuntime(clientServerJarFile, runtimeServerJarFile, "server jar file");

        File clientAOTFile = installExt.resolveClientAOTFile();
        File runtimeAOTFile = runtimeExt.resolveRuntimeAOTFile();
        FileUtils.copyClientFileToRuntime(clientAOTFile, runtimeAOTFile, "server aot file");

        File clientAssetsFile = installExt.resolveClientAssetsFile();
        File runtimeAssetsFile = runtimeExt.resolveRuntimeAssetsFile();
        FileUtils.copyClientFileToRuntime(clientAssetsFile, runtimeAssetsFile, "assets zip file");

        PluginLog.info("");
        PluginLog.info("Successfully setup server directory!");
        PluginLog.info(runtimeDirectory.getAbsolutePath());
        PluginLog.info("");
    }
}
