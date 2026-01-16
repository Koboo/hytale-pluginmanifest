package eu.koboo.pluginmanifest.gradle.plugin.tasks;

import eu.koboo.pluginmanifest.gradle.plugin.extension.AuthMode;
import eu.koboo.pluginmanifest.gradle.plugin.extension.serverdependency.ServerRuntimeExtension;
import eu.koboo.pluginmanifest.gradle.plugin.PluginLog;
import lombok.extern.slf4j.Slf4j;
import org.gradle.api.GradleException;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
public abstract class RunServerTask extends JavaExec {

    @Nested
    public abstract Property<ServerRuntimeExtension> getRuntimeExtension();

    @TaskAction
    public void runTask() {
        PluginLog.info("Building start command...");
        ServerRuntimeExtension runtimeExt = getRuntimeExtension().get();

        File runtimeDirectory = runtimeExt.resolveRuntimeDirectory();
        File runtimeServerJarFile = runtimeExt.resolveRuntimeServerJarFile();
        if(!runtimeServerJarFile.exists()) {
            throw new GradleException("Can't find server jar file: " + runtimeServerJarFile.getAbsolutePath());
        }
        File runtimeAssetsFile = runtimeExt.resolveRuntimeAssetsFile();
        if(!runtimeAssetsFile.exists()) {
            throw new GradleException("Can't find assets file: " + runtimeAssetsFile.getAbsolutePath());
        }
        File runtimeAOTFile = runtimeExt.resolveRuntimeAOTFile();
        if(!runtimeAOTFile.exists()) {
            throw new GradleException("Can't find AOT file: " + runtimeAOTFile.getAbsolutePath());
        }

        List<String> jvmArguments = new ArrayList<>();
        jvmArguments.add("-XX:AOTCache=" + runtimeAOTFile.getAbsolutePath());
        List<String> userJvmArguments = runtimeExt.getJvmArguments().get();
        if(!userJvmArguments.isEmpty()) {
            jvmArguments.addAll(userJvmArguments);
        }

        List<String> arguments = new ArrayList<>();
        if (runtimeExt.getDisableSentry().get()) {
            arguments.add("--disable-sentry");
        }
        if(runtimeExt.getAcceptEarlyPlugins().get()) {
            arguments.add("--accept-early-plugins");
        }
        if(runtimeExt.getAllowOp().get()) {
            arguments.add("--allow-op");
        }
        AuthMode authMode = runtimeExt.getAuthMode().get();
        arguments.add("--auth-mode");
        arguments.add(authMode.name().toLowerCase(Locale.ROOT));

        String bindAddress = runtimeExt.getBindAddress().get();
        arguments.add("--bind");
        arguments.add(bindAddress);

        arguments.add("--assets");
        arguments.add(runtimeAssetsFile.getAbsolutePath());

        List<String> userServerArguments = runtimeExt.getServerArguments().get();
        if(!userServerArguments.isEmpty()) {
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

        workingDir(runtimeDirectory);
        jvmArgs(jvmArguments);
        args(arguments);
        classpath(runtimeServerJarFile.getAbsolutePath());
        setStandardInput(System.in);
        setStandardOutput(System.out);
    }
}
