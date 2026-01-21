package eu.koboo.pluginmanifest.gradle.plugin.tasks;

import eu.koboo.pluginmanifest.gradle.plugin.utils.PluginLog;
import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import java.io.File;
import java.util.ArrayList;
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

    @Input
    public abstract Property<String> getRuntimeDirectory();

    @Input
    public abstract Property<Boolean> getAllowOp();

    @Input
    public abstract ListProperty<String> getJvmArguments();

    @Input
    public abstract ListProperty<String> getServerArguments();

    @TaskAction
    public void runTask() {
        PluginLog.info("Building start command...");
        File serverJarFile = getClientServerJarFile().getAsFile().getOrNull();
        if (serverJarFile == null || !serverJarFile.exists()) {
            throw new GradleException("HytaleServer.jar doesn't exist!");
        }
        File serverAOTFile = getClientAOTFile().getAsFile().getOrNull();
        if (serverAOTFile == null || !serverAOTFile.exists()) {
            throw new GradleException("HytaleServer.aot doesn't exist!");
        }
        File serverAssetsFile = getClientAssetsFile().getAsFile().getOrNull();
        if (serverAssetsFile == null || !serverAssetsFile.exists()) {
            throw new GradleException("Assets.zip doesn't exist!");
        }
        String runtimeDirectoryPath = getRuntimeDirectory().getOrNull();
        if (runtimeDirectoryPath == null || runtimeDirectoryPath.trim().isEmpty()) {
            throw new GradleException("runtimeDirectory can't be null or empty!");
        }
        File runtimeDirectory = new File(runtimeDirectoryPath);
        if (!runtimeDirectory.exists()) {
            runtimeDirectory.mkdirs();
        }

        List<String> jvmArguments = new ArrayList<>();
        jvmArguments.add("-XX:AOTCache=" + serverAOTFile.getAbsolutePath());
        jvmArguments.add("--enable-native-access=ALL-UNNAMED");
        List<String> userJvmArguments = getJvmArguments().getOrNull();
        if (userJvmArguments != null && !userJvmArguments.isEmpty()) {
            jvmArguments.addAll(userJvmArguments);
        }

        List<String> serverArguments = new ArrayList<>();
        // We disable sentry by default. Don't spam Hypixel, please.
        serverArguments.add("--disable-sentry");

        boolean allowOp = getAllowOp().getOrElse(true);
        if (allowOp) {
            serverArguments.add("--allow-op");
        }

        serverArguments.add("--assets");
        serverArguments.add(serverAssetsFile.getAbsolutePath());

        File archiveFile = getArchiveFile().getAsFile().getOrNull();
        if (archiveFile == null || !archiveFile.exists()) {
            throw new GradleException("Archive file doesn't exist!");
        }
        serverArguments.add("--mods");
        serverArguments.add(archiveFile.getParentFile().getAbsolutePath());

        List<String> userServerArguments = getServerArguments().getOrNull();
        if (userServerArguments != null && !userServerArguments.isEmpty()) {
            serverArguments.addAll(userServerArguments);
        }

        String jvmArgumentsString = String.join(" ", jvmArguments);
        String argumentsString = String.join(" ", serverArguments);
        String startCommand = "java " + jvmArgumentsString + " -jar HytaleServer.jar " + argumentsString;
        PluginLog.info("");
        PluginLog.info("Successfully build start command:");
        PluginLog.info(startCommand);
        PluginLog.info("");

        PluginLog.info("Starting development server...");

        workingDir(runtimeDirectory);
        classpath(serverJarFile);
        jvmArgs(jvmArguments);
        args(serverArguments);
        setStandardInput(System.in);
        setStandardOutput(System.out);
        setErrorOutput(System.err);
    }
}
