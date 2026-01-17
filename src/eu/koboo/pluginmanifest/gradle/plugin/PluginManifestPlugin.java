package eu.koboo.pluginmanifest.gradle.plugin;

import eu.koboo.pluginmanifest.gradle.plugin.extension.clientinstall.ClientInstallationExtension;
import eu.koboo.pluginmanifest.gradle.plugin.extension.manifest.JsonManifestExtension;
import eu.koboo.pluginmanifest.gradle.plugin.extension.serverruntime.ServerRuntimeExtension;
import eu.koboo.pluginmanifest.gradle.plugin.tasks.*;
import eu.koboo.pluginmanifest.gradle.plugin.utils.JarManifestUtils;
import eu.koboo.pluginmanifest.gradle.plugin.utils.JavaSourceUtils;
import eu.koboo.pluginmanifest.gradle.plugin.utils.PluginLog;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.DuplicatesStrategy;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.language.jvm.tasks.ProcessResources;
import org.gradle.process.ExecOperations;
import org.gradle.process.ExecResult;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class PluginManifestPlugin implements Plugin<Project> {

    private static final String EXTENSION_NAME = "pluginManifest";
    private static final String TASK_GROUP_NAME = EXTENSION_NAME.toLowerCase(Locale.ROOT);

    private static final String GENERATE_MANIFEST = "generateManifest";
    private static final String SETUP_SERVER = "setupServer";
    private static final String DELETE_SERVER = "deleteServer";
    private static final String UPDATE_SERVER = "updateServer";
    private static final String RUN_SERVER = "runServer";
    private static final String INSTALL_PLUGIN = "installPlugin";
    private static final String BUILD_AND_RUN = "buildAndRun";

    private static final String VINEFLOWER_URI = "https://github.com/Vineflower/vineflower/releases/download/1.11.2/vineflower-1.11.2-slim.jar";
    private static final String decompilePackage = "com/hypixel";

    private final ObjectFactory objectFactory;
    private final ExecOperations execOperations;

    @Inject
    public PluginManifestPlugin(ObjectFactory objectFactory, ExecOperations execOperations) {
        this.objectFactory = objectFactory;
        this.execOperations = execOperations;
    }

    @Override
    public void apply(Project target) {
        PluginManifestExtension extension = target.getExtensions().create(EXTENSION_NAME, PluginManifestExtension.class);
        TaskProvider<GenerateManifestTask> generateManifestProvider = target.getTasks().register(GENERATE_MANIFEST, GenerateManifestTask.class);
        TaskProvider<SetupServerTask> setupServerProvider = target.getTasks().register(SETUP_SERVER, SetupServerTask.class);
        TaskProvider<DeleteServerTask> deleteServerProvider = target.getTasks().register(DELETE_SERVER, DeleteServerTask.class);
        TaskProvider<UpdateServerTask> updateServerProvider = target.getTasks().register(UPDATE_SERVER, UpdateServerTask.class);
        TaskProvider<RunServerTask> runServerProvider = target.getTasks().register(RUN_SERVER, RunServerTask.class);
        TaskProvider<InstallPluginTask> installPluginProvider = target.getTasks().register(INSTALL_PLUGIN, InstallPluginTask.class);

        target.getTasks().register(BUILD_AND_RUN, Task.class, task -> {
            task.setGroup(TASK_GROUP_NAME);
            task.setDescription("Builds your plugin and starts the server.");
            task.dependsOn(installPluginProvider, runServerProvider);
        });

        target.afterEvaluate(project -> {
            JsonManifestExtension manifestExt = extension.jsonManifestExtension;
            ServerRuntimeExtension runtimeExt = extension.serverRuntimeExtension;
            ClientInstallationExtension installExt = extension.installationExtension;

            // Resolve client installation directory
            File clientServerJarFile = installExt.resolveClientServerJarFile();
            if (!clientServerJarFile.exists()) {
                throw new GradleException("Can't find server jar file at " + clientServerJarFile.getAbsolutePath());
            }
            decompileServerSource(installExt);

            File archiveFile = JavaSourceUtils.resolveArchive(project);
            String archiveTaskName = JavaSourceUtils.resolveArchiveTaskName(project);

            // Applying server dependency as a file.
            project.getRepositories().flatDir(repository ->
                repository.dirs(installExt.resolveClientServerDirectory())
            );
            project.getDependencies().add(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME, ":HytaleServer");

            // Depend "processResources" on "generateManifest"
            // Exclude manual "manifest.json"
            target.getTasks().withType(ProcessResources.class).configureEach(task -> {
                task.dependsOn(generateManifestProvider);
                task.doLast(t -> {
                    t.getOutputs().getFiles().forEach(outputFile -> {
                        if(!outputFile.getName().endsWith("manifest.json")) {
                            return;
                        }
                        task.setDuplicatesStrategy(DuplicatesStrategy.INCLUDE);
                    });
                });
            });

            // Adding PROJECT/build/generated/pluginmanifest/ to sourceSet resources.
            File buildDirectory = project
                .getLayout()
                .getBuildDirectory()
                .get()
                .getAsFile();
            SourceSet mainSourceSet = project.getExtensions()
                .getByType(SourceSetContainer.class)
                .getByName("main");

            // Check if the plugin has any resources BEFORE applying our directory to the sourceSet.
            // Otherwise, the boolean would be true because it treats our directory as a resource too.
            boolean hasAnyResources = JavaSourceUtils.hasAnyResource(mainSourceSet);

            File generatedResourceDirectory = new File(buildDirectory, "generated/pluginmanifest/");
            mainSourceSet.getResources().srcDir(generatedResourceDirectory);

            List<String> mainClassCandidates = JavaSourceUtils.getMainClassCandidates(mainSourceSet);

            // Configure "generateManifestJson"
            generateManifestProvider.configure(task -> {
                task.setGroup(TASK_GROUP_NAME);
                task.setDescription("Generates the manifest.json and puts into plugins jar file.");
                task.getResourceDirectory().set(generatedResourceDirectory.getAbsolutePath());
                task.getProjectGroupId().set(project.getGroup().toString());
                task.getProjectArtifactId().set(project.getName());
                task.getProjectVersion().set(project.getVersion().toString());
                task.getMainClassCandidates().set(mainClassCandidates);
                task.getOSUserName().set(System.getProperty("user.name"));
                task.getExtension().set(manifestExt);
                task.getOutputs().dir(generatedResourceDirectory);
            });

            // Configure "setupServer"
            setupServerProvider.configure(task -> {
                task.setGroup(TASK_GROUP_NAME);
                task.setDescription("Sets up the provided server directory with the server jar from your client–installation.");
                task.getRuntimeExtension().set(runtimeExt);
                task.getInstallationExtension().set(installExt);
            });

            // Configure "deleteServer"
            deleteServerProvider.configure(task -> {
                task.setGroup(TASK_GROUP_NAME);
                task.setDescription("Deletes the provided server directory.");
                task.getRuntimeExtension().set(runtimeExt);
            });

            // Configure "updateServer"
            updateServerProvider.configure(task -> {
                task.setGroup(TASK_GROUP_NAME);
                task.setDescription("Updates \"HytaleServer.jar\", \"HytaleServer.aot\" and \"Assets.zip\" in your server directory from your local client-installation.");
                task.getRuntimeExtension().set(runtimeExt);
                task.getInstallExtension().set(installExt);
            });

            // Configure "runServer"
            runServerProvider.configure(task -> {
                task.setGroup(TASK_GROUP_NAME);
                task.setDescription("Runs the server in your server directory with console support.");
                task.getRuntimeExtension().set(runtimeExt);
                task.getInstallExtension().set(installExt);
                task.dependsOn(project.getTasks().getByName(SETUP_SERVER));
            });

            // Configure "installPlugin"
            installPluginProvider.configure(task -> {
                task.setGroup(TASK_GROUP_NAME);
                task.setDescription("Installs and builds your plugin and moves it to your server directory.");
                task.getRuntimeExtension().set(runtimeExt);
                task.getArchiveFilePath().set(archiveFile.getAbsolutePath());
                task.dependsOn(project.getTasks().getByName(archiveTaskName));
            });

            //
            // ==== INFORMATION PRINTING ====
            //

            String mainClass = "Not found";
            if (mainClassCandidates.size() == 1) {
                mainClass = mainClassCandidates.getFirst();
            }

            PluginDoctor.printDoctor(project, runtimeExt, installExt, hasAnyResources, mainClass);
        });
    }

    private void decompileServerSource(ClientInstallationExtension installExt) {
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
                return;
            }
            PluginLog.info("Sources file is outdated!");
            clientServerSourcesFile.delete();
            PluginLog.info("Deleted previous sources file.");
        }
        PluginLog.info("Decompiling server sources...");
        File clientServerServerDirectory = clientServerJarFile.getParentFile();

        File vineFlowerJarFile = new File(clientServerSourcesFile.getParent(), "vineflower.jar");
        if (!vineFlowerJarFile.exists()) {
            PluginLog.info("Downloading vineflower from: " + VINEFLOWER_URI);
            try {
                Files.copy(URI.create(VINEFLOWER_URI).toURL().openStream(), vineFlowerJarFile.toPath());
            } catch (IOException e) {
                throw new GradleException("Can't decompile server: ", e);
            }
            PluginLog.info("Downloaded vineflower to: " + vineFlowerJarFile.getAbsolutePath());
        }

        List<String> decompileArguments = List.of(
            "--only=" + decompilePackage,
            "--simplify-switch=1",
            "--decompile-generics=1",
            "--remove-synthetic=0",
            "--remove-bridge=1",
            clientServerJarFile.getAbsolutePath(),
            clientServerSourcesFile.getAbsolutePath()
        );

        PluginLog.info("Decompiling server sources.. ");
        PluginLog.info("Please wait..");
        PluginLog.info("This may take a few minutes..");
        ExecResult result = execOperations.javaexec(spec -> {
            spec.setWorkingDir(clientServerServerDirectory);
            spec.setClasspath(objectFactory.fileCollection().from(vineFlowerJarFile));
            spec.setArgs(decompileArguments);
            spec.setStandardInput(InputStream.nullInputStream());
            spec.setStandardOutput(OutputStream.nullOutputStream());
            spec.setErrorOutput(OutputStream.nullOutputStream());
        });

        PluginLog.info("");
        PluginLog.info("Decompiled server sources resulted in exitCode=" + result.getExitValue());
        PluginLog.info(clientServerSourcesFile.getAbsolutePath());
        PluginLog.info("");
    }
}
