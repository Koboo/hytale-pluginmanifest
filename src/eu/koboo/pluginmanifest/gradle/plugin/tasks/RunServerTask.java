package eu.koboo.pluginmanifest.gradle.plugin.tasks;

import eu.koboo.pluginmanifest.gradle.plugin.utils.FileUtils;
import eu.koboo.pluginmanifest.gradle.plugin.utils.PluginLog;
import lombok.SneakyThrows;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.gradle.work.DisableCachingByDefault;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@DisableCachingByDefault(because = "Starts the configured hytale server")
public abstract class RunServerTask extends JavaExec {

    @InputFile
    public abstract RegularFileProperty getClientServerJarFile();

    @InputFile
    public abstract RegularFileProperty getClientAOTFile();

    @InputFile
    public abstract RegularFileProperty getClientAssetsFile();

    @InputFile
    public abstract RegularFileProperty getArchiveFile();

    @InputDirectory
    public abstract DirectoryProperty getRuntimeDirectory();

    @Input
    public abstract Property<Boolean> getCopyPluginToRuntime();

    @Input
    public abstract Property<Boolean> getDeleteLogsOnStart();

    @Input
    public abstract Property<Boolean> getAllowOp();

    @Input
    public abstract ListProperty<String> getUserJvmArguments();

    @Input
    public abstract ListProperty<String> getUserServerArguments();

    @TaskAction
    public void runTask() {
        PluginLog.info("Building start command...");
        File serverJarFile = getClientServerJarFile().getAsFile().get();
        if (!serverJarFile.exists()) {
            throw new StopExecutionException("HytaleServer.jar doesn't exist!");
        }
        File runtimeDirectory = getRuntimeDirectory().getAsFile().get();
        if (!runtimeDirectory.exists()) {
            runtimeDirectory.mkdirs();
        }
        File runtimeServerJar = new File(runtimeDirectory, "HytaleServer.jar");
        if(runtimeServerJar.exists()) {
            serverJarFile = runtimeServerJar;
            PluginLog.info("Using server jar from runtime directory");
        }

        File logsDirectory = new File(runtimeDirectory, "logs");
        if(logsDirectory.exists()) {
            try {
                FileUtils.deleteRecursively(logsDirectory.toPath());
            } catch (IOException e) {
                PluginLog.info("Can not delete runtime logs directory: " + e.getMessage());
            }
        }

        List<String> taskJvmArguments = new ArrayList<>();

        // Add optional aot file for faster startup
        File serverAOTFile = getClientAOTFile().getAsFile().get();
        if (serverAOTFile.exists()) {
            taskJvmArguments.add("-XX:AOTCache=" + serverAOTFile.getAbsolutePath());
        }

        // Disable warnings
        taskJvmArguments.add("--enable-native-access=ALL-UNNAMED");

        // Include users jvm arguments
        List<String> userJvmArguments = getUserJvmArguments().getOrNull();
        if (userJvmArguments != null && !userJvmArguments.isEmpty()) {
            taskJvmArguments.addAll(userJvmArguments);
        }

        List<String> taskServerArguments = new ArrayList<>();

        // Disable sentry by default
        taskServerArguments.add("--disable-sentry");

        // Adding self-op command allowance
        boolean allowOp = getAllowOp().getOrElse(true);
        if (allowOp) {
            taskServerArguments.add("--allow-op");
        }

        // Adding assets.zip arguments
        File serverAssetsFile = getClientAssetsFile().getAsFile().getOrNull();
        if (serverAssetsFile == null || !serverAssetsFile.exists()) {
            throw new StopExecutionException("Assets.zip doesn't exist!");
        }
        taskServerArguments.add("--assets");
        taskServerArguments.add(serverAssetsFile.getAbsolutePath());

        // Adding plugin by copying or including build directory.
        File pluginArchiveFile = getArchiveFile().getAsFile().getOrNull();
        if (pluginArchiveFile == null || !pluginArchiveFile.exists()) {
            throw new StopExecutionException("Archive file doesn't exist!");
        }
        File modsDirectory = new File(runtimeDirectory, "mods");
        if (!modsDirectory.exists()) {
            modsDirectory.mkdirs();
        }
        File modsArchiveFile = new File(modsDirectory, pluginArchiveFile.getName());
        boolean copyPluginToRuntimeModsFolder = getCopyPluginToRuntime().get();

        // Check if there are more than 1 jar file inside "build/libs/"
        File buildLibsDirectory = pluginArchiveFile.getParentFile();
        File[] archiveFiles =  buildLibsDirectory.listFiles();
        if(archiveFiles != null && archiveFiles.length > 1) {
            long archiveFileAmount = Arrays.stream(archiveFiles)
                .map(File::getName)
                .filter(name -> name.endsWith(".jar") || name.endsWith(".zip"))
                .count();
            PluginLog.info("Found " + archiveFileAmount + " jar files inside build directory..");
            copyPluginToRuntimeModsFolder = archiveFileAmount > 1;
            if(copyPluginToRuntimeModsFolder) {
                PluginLog.info("Detected more than one jar file in build directory!");
            }
        }

        if (!copyPluginToRuntimeModsFolder) {
            PluginLog.info("Using \"--mods\" as argument to include build directory as mods!");
            if (modsArchiveFile.exists()) {
                modsArchiveFile.delete();
                PluginLog.info("Deleted existing plugin from runtime \"mods/\" directory!");
            }

            taskServerArguments.add("--mods");
            taskServerArguments.add(buildLibsDirectory.getAbsolutePath());
        } else {
            PluginLog.info("Copying plugin into runtime \"mods/\" directory!");
            try {
                FileUtils.copyFileTo(pluginArchiveFile, modsArchiveFile);
            } catch (IOException e) {
                throw new StopExecutionException("Can't copy plugin archive into runtime: " + e.getMessage());
            }
        }

        // Include users server arguments
        List<String> userServerArguments = getUserServerArguments().getOrNull();
        if (userServerArguments != null && !userServerArguments.isEmpty()) {
            taskServerArguments.addAll(userServerArguments);
        }

        String jvmArgumentsString = String.join(" ", taskJvmArguments);
        String argumentsString = String.join(" ", taskServerArguments);
        String serverJarFilePath = serverJarFile.getAbsolutePath();
        String startCommand = "java " + jvmArgumentsString + " -jar " + serverJarFilePath + " " + argumentsString;
        PluginLog.info("");
        PluginLog.info("Successfully parsed arguments:");
        PluginLog.info("");
        PluginLog.info("JavaVM arguments:");
        PluginLog.info(jvmArgumentsString);
        PluginLog.info("");
        PluginLog.info("Server arguments:");
        PluginLog.info(argumentsString);
        PluginLog.info("");
        PluginLog.info("Start command:");
        PluginLog.info(startCommand);
        PluginLog.info("");

        PluginLog.info("Starting development server...");

        workingDir(runtimeDirectory);
        classpath(serverJarFile);
        jvmArgs(taskJvmArguments);
        args(taskServerArguments);
        setStandardInput(System.in);
        setStandardOutput(System.out);
        setErrorOutput(System.err);
    }
}
