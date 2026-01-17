package eu.koboo.pluginmanifest.gradle.plugin.tasks;

import eu.koboo.pluginmanifest.gradle.plugin.extension.clientinstall.ClientInstallationExtension;
import eu.koboo.pluginmanifest.gradle.plugin.extension.serverruntime.ServerRuntimeExtension;
import eu.koboo.pluginmanifest.gradle.plugin.utils.JarManifestUtils;
import eu.koboo.pluginmanifest.gradle.plugin.utils.PluginLog;
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
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.List;

public abstract class DecompileServerTask extends DefaultTask {

    private static final String VINEFLOWER_URI = "https://github.com/Vineflower/vineflower/releases/download/1.11.2/vineflower-1.11.2-slim.jar";
    private static final String decompilePackage = "com/hypixel";

    private final ObjectFactory objectFactory;
    private final ExecOperations execOperations;

    @Inject
    public DecompileServerTask(ObjectFactory objectFactory, ExecOperations execOperations) {
        this.objectFactory = objectFactory;
        this.execOperations = execOperations;
    }

    @Nested
    public abstract Property<ServerRuntimeExtension> getRuntimeExtension();

    @Nested
    public abstract Property<ClientInstallationExtension> getInstallExtension();

    @TaskAction
    public void runTask() throws IOException {
        PluginLog.info("Decompiling server sources...");
        ClientInstallationExtension installExt = getInstallExtension().get();

        File clientServerJarFile = installExt.resolveClientServerJarFile();
        if (!clientServerJarFile.exists()) {
            throw new GradleException("Can't decompile server, because jar file doesn't exist: " + clientServerJarFile.getAbsolutePath());
        }
        File clientServerSourcesFile = installExt.resolveClientServerSourcesFile();
        if (clientServerSourcesFile.exists()) {
            String jarVersion = JarManifestUtils.getVersion(clientServerJarFile);
            if (JarManifestUtils.isUnknown(jarVersion)) {
                throw new GradleException("Couldn't check compiled jar version: " + clientServerSourcesFile.getAbsolutePath());
            }
            String sourcesVersion = JarManifestUtils.getVersion(clientServerSourcesFile);
            if (JarManifestUtils.isUnknown(sourcesVersion)) {
                throw new GradleException("Couldn't check sources jar version: " + clientServerSourcesFile.getAbsolutePath());
            }
            if (jarVersion.equals(sourcesVersion)) {
                PluginLog.info("Sources file is up-to-date: " + clientServerSourcesFile.getAbsolutePath());
                return;
            }
            PluginLog.info("Sources file is outdated: " + clientServerSourcesFile.getAbsolutePath());
            clientServerSourcesFile.delete();
            PluginLog.info("Deleted previous sources file.");
        }
        File clientServerServerDirectory = clientServerJarFile.getParentFile();

        File vineFlowerJarFile = new File(clientServerSourcesFile.getParent(), "vineflower.jar");
        if (!vineFlowerJarFile.exists()) {
            PluginLog.info("Downloading vineflower from: " + VINEFLOWER_URI);
            Files.copy(URI.create(VINEFLOWER_URI).toURL().openStream(), vineFlowerJarFile.toPath());
            PluginLog.info("Downloaded vineflower to: " + vineFlowerJarFile.getAbsolutePath());
        }

        PluginLog.info("Starting server decompilation...");

        List<String> decompileArguments = List.of(
            "--only=" + decompilePackage,
            "--simplify-switch=1",
            "--decompile-generics=1",
            "--remove-synthetic=0",
            "--remove-bridge=1",
            clientServerJarFile.getAbsolutePath(),
            clientServerSourcesFile.getAbsolutePath()
        );

        ExecResult result = execOperations.javaexec(spec -> {
            spec.setWorkingDir(clientServerServerDirectory);
            spec.setClasspath(objectFactory.fileCollection().from(vineFlowerJarFile));
            spec.setArgs(decompileArguments);
            spec.setStandardInput(System.in);
            spec.setStandardOutput(System.out);
            spec.setErrorOutput(System.err);
        });

        PluginLog.info("");
        PluginLog.info("Successfully decompiled server sources: (exitCode=" + result.getExitValue() + ")");
        PluginLog.info(clientServerSourcesFile.getAbsolutePath());
        PluginLog.info("");
    }
}
