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
import org.gradle.api.*;
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
import java.util.jar.Manifest;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class PluginManifestPlugin implements Plugin<Project> {

    public static final String EXTENSION_NAME = "pluginManifest";
    public static final String TASK_GROUP_NAME = EXTENSION_NAME.toLowerCase(Locale.ROOT);

    public static final String GENERATE_MANIFEST = "generateManifest";
    public static final String SETUP_SERVER = "setupServer";
    public static final String DELETE_SERVER = "deleteServer";
    public static final String UPDATE_SERVER = "updateServer";
    public static final String RUN_SERVER = "runServer";
    public static final String INSTALL_PLUGIN = "installPlugin";
    public static final String BUILD_AND_RUN = "buildAndRun";

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

            File clientInstallationDirectory = installExt.resolveClientInstallDirectory();
            File clientServerDirectory = installExt.resolveClientServerDirectory();

            File archiveFile = JavaSourceUtils.resolveArchive(project);
            String archiveTaskName = JavaSourceUtils.resolveArchiveTaskName(project);

            File clientAssetsFile = installExt.resolveClientAssetsFile();
            File clientAOTFile = installExt.resolveClientAOTFile();

            // Applying server dependency as a file.
            project.getRepositories().flatDir(repository ->
                repository.dirs(clientServerDirectory)
            );
            project.getDependencies().add(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME, ":HytaleServer");

            // Adding PROJECT/build/generated/pluginmanifest/ to sourceSet resources.
            File buildDirectory = project
                .getLayout()
                .getBuildDirectory()
                .get()
                .getAsFile();
            File generatedResourceDirectory = new File(buildDirectory, "generated/pluginmanifest/");
            SourceSet mainSourceSet = project.getExtensions()
                .getByType(SourceSetContainer.class)
                .getByName("main");

            // Resolve existing resources BEFORE applying our manifest-parent.
            boolean hasAnyResources = JavaSourceUtils.hasAnyResource(mainSourceSet);
            mainSourceSet.getResources().srcDir(generatedResourceDirectory);

            List<String> mainClassCandidates = JavaSourceUtils.getMainClassCandidates(mainSourceSet);
            boolean hasMainClass = mainClassCandidates.size() == 1;

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
            // Depend "generateManifest" on "processResources"
            // Exclude manual "manifest.json"
            project.getTasks()
                .withType(ProcessResources.class)
                .configureEach(task -> {
                    task.dependsOn(generateManifestProvider);
                    task.eachFile(file -> {
                        if (!file.getName().equals("manifest.json")) {
                            return;
                        }
                        file.setDuplicatesStrategy(DuplicatesStrategy.INCLUDE);
                    });
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

            // Parse runtime directory
            String infoRuntimeDirectory = "Not configured";
            String runtimeDirectoryPath = runtimeExt.getRuntimeDirectory().getOrNull();
            File runtimeDirectory = null;
            if (runtimeDirectoryPath != null && !runtimeDirectoryPath.trim().isEmpty()) {
                runtimeDirectory = new File(runtimeDirectoryPath);
                infoRuntimeDirectory = runtimeDirectory.getAbsolutePath();
            }
            // Parse patchline name
            String pathlineName = installExt.getPatchline().get().name().toLowerCase(Locale.ROOT);

            boolean isServerRunnable = false;
            File runtimeServerJarFile = null;
            if (runtimeDirectory != null && runtimeDirectory.exists()) {
                runtimeServerJarFile = runtimeExt.resolveRuntimeServerJarFile();
                File runtimeAOTFile = runtimeExt.resolveRuntimeAOTFile();
                File runtimeAssetsFile = runtimeExt.resolveRuntimeAssetsFile();
                if (runtimeServerJarFile.exists() && runtimeAOTFile.exists() && runtimeAssetsFile.exists()) {
                    isServerRunnable = true;
                }
            }

            // Parse versions by MANIFEST of client and runtime server jar
            String matchesVersion = "NO";
            String clientServerVersion = JarManifestUtils.getVersion(clientServerJarFile);
            String runtimeServerVersion = JarManifestUtils.getVersion(runtimeServerJarFile);
            if (!JarManifestUtils.isUnknown(clientServerVersion) && !JarManifestUtils.isUnknown(runtimeServerVersion)) {
                if(clientServerVersion.equals(runtimeServerVersion)) {
                    matchesVersion = "YES";
                }
            } else {
                matchesVersion = "Both unknown";
            }

            String mainClass = "Not found";
            if(mainClassCandidates.size() == 1) {
                mainClass =  mainClassCandidates.getFirst();
            }

            File clientServerSourcesFile = installExt.resolveClientServerSourcesFile();

            PluginLog.info("========= Files ========");
            PluginLog.info("  client-installation path > " + clientInstallationDirectory.getAbsolutePath());
            PluginLog.info("                 Patchline > " + pathlineName);
            PluginLog.info("            Server-Version > " + clientServerVersion);
            PluginLog.info("        'HytaleServer.jar' > " + fileExists(clientServerJarFile));
            PluginLog.info("        'HytaleServer.aot' > " + fileExists(clientAOTFile));
            PluginLog.info("'HytaleServer-sources.jar' > " + fileExists(clientServerSourcesFile));
            PluginLog.info("              'Assets.zip' > " + fileExists(clientAssetsFile));
            PluginLog.info("====== Manifest =======");
            PluginLog.info("       'IncludesAssetPack' > \"" + hasAnyResources + "\"");
            PluginLog.info("                    'Main' > \"" + mainClass + "\"");
            PluginLog.info("====== JAR file =======");
            PluginLog.info("       JAR file build task > \"" + archiveTaskName + "\"");
            PluginLog.info("       JAR file build name > " + archiveFile.getName());
            PluginLog.info("       JAR file build path > " + archiveFile.getAbsolutePath());
            PluginLog.info("======= Runtime ========");
            PluginLog.info("  Server-Runtime-Directory > " + infoRuntimeDirectory);
            PluginLog.info("    Runtime-Server-Version > " + runtimeServerVersion);
            PluginLog.info("       Is server runnable? > " + booleanToHuman(isServerRunnable));
            PluginLog.info("   Version matches client? > " + matchesVersion);
        });
    }

    private String booleanToHuman(boolean value) {
        return value ? "YES" : "NO";
    }

    private String fileExists(File file) {
        if(file == null) {
            return "Not found";
        }
        return file.exists() ? "Found" : "Not found";
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
