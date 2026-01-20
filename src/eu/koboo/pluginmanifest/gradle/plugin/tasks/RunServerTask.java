package eu.koboo.pluginmanifest.gradle.plugin.tasks;

import eu.koboo.pluginmanifest.gradle.plugin.extension.clientinstall.ClientInstallationExtension;
import eu.koboo.pluginmanifest.gradle.plugin.extension.serverruntime.ServerRuntimeExtension;
import eu.koboo.pluginmanifest.gradle.plugin.utils.FileUtils;
import eu.koboo.pluginmanifest.gradle.plugin.utils.PluginLog;
import lombok.extern.slf4j.Slf4j;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.ExecOperations;
import org.gradle.process.ExecResult;

import javax.inject.Inject;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public abstract class RunServerTask extends DefaultTask {

    @Nested
    public abstract Property<ServerRuntimeExtension> getRuntimeExtension();

    @Nested
    public abstract Property<ClientInstallationExtension> getInstallExtension();

    @Inject
    public abstract ObjectFactory getObjects();

    @Inject
    public abstract ExecOperations getExecOperations();

    @TaskAction
    public void runTask() {
        PluginLog.info("Building start command...");
        ServerRuntimeExtension runtimeExt = getRuntimeExtension().get();
        ClientInstallationExtension installExt = getInstallExtension().get();

        File clientServerJarFile = installExt.resolveClientServerJarFile();
        File runtimeServerJarFile = runtimeExt.resolveRuntimeServerJarFile();
        FileUtils.copyClientFileToRuntime(clientServerJarFile, runtimeServerJarFile, "server jar file");

        File clientAOTFile = installExt.resolveClientAOTFile();
        File runtimeAOTFile = runtimeExt.resolveRuntimeAOTFile();
        FileUtils.copyClientFileToRuntime(clientAOTFile, runtimeAOTFile, "server aot file");

        File clientAssetsFile = installExt.resolveClientAssetsFile();
        File runtimeAssetsFile = runtimeExt.resolveRuntimeAssetsFile();
        FileUtils.copyClientFileToRuntime(clientAssetsFile, runtimeAssetsFile, "assets zip file");

        if (!runtimeServerJarFile.exists()) {
            throw new GradleException("Can't find server jar file: " + runtimeServerJarFile.getAbsolutePath());
        }
        if (!runtimeAssetsFile.exists()) {
            throw new GradleException("Can't find assets file: " + runtimeAssetsFile.getAbsolutePath());
        }
        if (!runtimeAOTFile.exists()) {
            throw new GradleException("Can't find AOT file: " + runtimeAOTFile.getAbsolutePath());
        }

        List<String> jvmArguments = new ArrayList<>();
        jvmArguments.add("-XX:AOTCache=" + runtimeAOTFile.getAbsolutePath());
        jvmArguments.add("--enable-native-access=ALL-UNNAMED");
        List<String> userJvmArguments = runtimeExt.getJvmArguments().get();
        if (!userJvmArguments.isEmpty()) {
            jvmArguments.addAll(userJvmArguments);
        }

        List<String> arguments = new ArrayList<>();
        // We disable sentry by default. Don't spam Hypixel, please.
        arguments.add("--disable-sentry");
        if (runtimeExt.getAcceptEarlyPlugins().get()) {
            arguments.add("--accept-early-plugins");
        }
        if (runtimeExt.getAllowOp().get()) {
            arguments.add("--allow-op");
        }

        String bindAddress = runtimeExt.getBindAddress().get();
        arguments.add("--bind");
        arguments.add(bindAddress);

        arguments.add("--assets");
        arguments.add(runtimeAssetsFile.getAbsolutePath());

        List<String> userServerArguments = runtimeExt.getServerArguments().get();
        if (!userServerArguments.isEmpty()) {
            arguments.addAll(userServerArguments);
        }

        String jvmArgumentsString = String.join(" ", jvmArguments);
        String argumentsString = String.join(" ", arguments);
        String startCommand = "java " + jvmArgumentsString + " -jar HytaleServer.jar " + argumentsString;
        PluginLog.info("");
        PluginLog.info("Successfully build start command:");
        PluginLog.info(startCommand);
        PluginLog.info("");

        PluginLog.info("Starting development server...");

        ExecResult result = getExecOperations().javaexec(spec -> {
            spec.setWorkingDir(runtimeExt.resolveRuntimeDirectory());
            spec.setClasspath(getObjects().fileCollection().from(runtimeServerJarFile));
            spec.setJvmArgs(jvmArguments);
            spec.setArgs(arguments);
            spec.setStandardInput(System.in);
            spec.setStandardOutput(System.out);
            spec.setErrorOutput(System.err);
        });

        PluginLog.info("");
        PluginLog.info("Server stopped. exitCode=" + result.getExitValue());
        PluginLog.info("");
    }
}
