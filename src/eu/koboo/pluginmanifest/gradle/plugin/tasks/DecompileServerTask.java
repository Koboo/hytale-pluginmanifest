package eu.koboo.pluginmanifest.gradle.plugin.tasks;

import eu.koboo.pluginmanifest.gradle.plugin.utils.FileUtils;
import eu.koboo.pluginmanifest.gradle.plugin.utils.PluginLog;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.gradle.work.DisableCachingByDefault;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@DisableCachingByDefault(because = "Starts the configured hytale server")
public abstract class DecompileServerTask extends JavaExec {

    private static final String VINEFLOWER_DOWNLOAD =
        "https://github.com/Vineflower/vineflower/releases/download/1.11.2/vineflower-1.11.2-slim.jar";

    @InputFile
    public abstract RegularFileProperty getClientServerJarFile();

    @TaskAction
    public void runTask() {
        PluginLog.info("Decompiling server sources...");
        File serverJarFile = getClientServerJarFile().getAsFile().getOrNull();
        if (serverJarFile == null || !serverJarFile.exists()) {
            throw new StopExecutionException("HytaleServer.jar doesn't exist!");
        }

        File serverDirectory = serverJarFile.getParentFile();

        File serverSourcesFile = new File(serverDirectory, "HytaleServer-sources.jar");
        if(serverSourcesFile.exists()) {
            PluginLog.info("Deleting old server sources...");
            serverSourcesFile.delete();
        }

        File vineflowerJarFile = new File(serverDirectory, "vineflower.jar");
        if(!vineflowerJarFile.exists()) {
            PluginLog.info("Downloading vineflower...");
            try {
                Files.copy(URI.create(VINEFLOWER_DOWNLOAD).toURL().openStream(), vineflowerJarFile.toPath());
                PluginLog.info("Downloaded vineflower!");
            } catch (IOException e) {
                throw new StopExecutionException("Can not download vineflower: " + e.getMessage());
            }
        }

        List<String> arguments = List.of(
            "--only=com/hypixel/",
            "HytaleServer.jar",
            "HytaleServer-sources.jar"
        );

        List<String> jvmArguments = List.of(
            "-Xms2G", "-Xmx4G"
        );

        PluginLog.info("Start decompiling...");

        workingDir(serverDirectory);
        classpath(vineflowerJarFile);
        jvmArgs(jvmArguments);
        args(arguments);
        setStandardInput(System.in);
        setStandardOutput(System.out);
        setErrorOutput(System.err);
    }
}
