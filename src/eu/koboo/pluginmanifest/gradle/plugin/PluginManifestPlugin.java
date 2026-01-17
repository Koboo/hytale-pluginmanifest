package eu.koboo.pluginmanifest.gradle.plugin;

import eu.koboo.pluginmanifest.gradle.plugin.extension.clientinstall.ClientInstallationExtension;
import eu.koboo.pluginmanifest.gradle.plugin.extension.manifest.ManifestExtension;
import eu.koboo.pluginmanifest.gradle.plugin.extension.serverdependency.ServerRuntimeExtension;
import eu.koboo.pluginmanifest.gradle.plugin.tasks.*;
import eu.koboo.pluginmanifest.gradle.plugin.utils.JarManifestUtils;
import eu.koboo.pluginmanifest.gradle.plugin.utils.JavaSourceUtils;
import eu.koboo.pluginmanifest.gradle.plugin.utils.PluginLog;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.gradle.api.*;
import org.gradle.api.file.DuplicatesStrategy;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.language.jvm.tasks.ProcessResources;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.jar.Manifest;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class PluginManifestPlugin implements Plugin<Project> {

    public static final String MAVEN_PUBLISH = "maven-publish";
    public static final String HYTALE_GROUP = "com.hypixel.hytale";
    public static final String HYTALE_ARTIFACT = "Server";

    public static final String EXTENSION_NAME = "pluginManifest";
    public static final String TASK_GROUP_NAME = EXTENSION_NAME.toLowerCase(Locale.ROOT);

    public static final String GENERATE_MANIFEST = "generateManifest";
    public static final String SETUP_SERVER = "setupServer";
    public static final String DELETE_SERVER = "deleteServer";
    public static final String UPDATE_SERVER = "updateServer";
    public static final String RUN_SERVER = "runServer";
    public static final String INSTALL_PLUGIN = "installPlugin";
    public static final String DECOMPILE_SERVER = "decompileServer";
    public static final String BUILD_AND_RUN = "buildAndRun";

    @Override
    public void apply(Project target) {
        PluginManifestExtension extension = target.getExtensions().create(EXTENSION_NAME, PluginManifestExtension.class);
        TaskProvider<GenerateManifestTask> generateManifestProvider = target.getTasks().register(GENERATE_MANIFEST, GenerateManifestTask.class);
        TaskProvider<SetupServerTask> setupServerProvider = target.getTasks().register(SETUP_SERVER, SetupServerTask.class);
        TaskProvider<DeleteServerTask> deleteServerProvider = target.getTasks().register(DELETE_SERVER, DeleteServerTask.class);
        TaskProvider<UpdateServerTask> updateServerProvider = target.getTasks().register(UPDATE_SERVER, UpdateServerTask.class);
        TaskProvider<RunServerTask> runServerProvider = target.getTasks().register(RUN_SERVER, RunServerTask.class);
        TaskProvider<DecompileServerTask> decompileServer = target.getTasks().register(DECOMPILE_SERVER, DecompileServerTask.class);
        TaskProvider<InstallPluginTask> installPluginProvider = target.getTasks().register(INSTALL_PLUGIN, InstallPluginTask.class);

        target.getTasks().register(BUILD_AND_RUN, Task.class, task -> {
            task.setGroup(TASK_GROUP_NAME);
            task.setDescription("Builds your plugin and starts the server.");
            task.dependsOn(installPluginProvider, runServerProvider);
        });

        target.afterEvaluate(project -> {
            ManifestExtension manifestExt = extension.manifestExtension;
            ServerRuntimeExtension runtimeExt = extension.serverRuntimeExtension;
            ClientInstallationExtension installExt = extension.installationExtension;

            // Resolve client installation directory
            File clientServerJarFile = installExt.resolveClientServerJarFile();
            if (!clientServerJarFile.exists()) {
                throw new GradleException("Can't find server jar file at " + clientServerJarFile.getAbsolutePath());
            }
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

            // Configure "decompileServer"
            decompileServer.configure(task -> {
                task.setGroup(TASK_GROUP_NAME);
                task.setDescription("Decompiles the HytaleServer.jar and puts a separate jar into the client's installation.");
                task.getInstallExtension().set(installExt);
            });

            // Execute "decompileServer" on gradle resync
            target.getPlugins().apply("idea");
            target.getTasks().named("ideaModule", task -> {
                task.dependsOn(decompileServer);
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
            boolean matchesVersion = true;
            Manifest clientServerManifest = JarManifestUtils.getManifest(clientServerJarFile);
            Manifest runtimeServerManifest = JarManifestUtils.getManifest(runtimeServerJarFile);
            String clientServerVersion = JarManifestUtils.getVersion(clientServerManifest);
            String runtimeServerVersion = JarManifestUtils.getVersion(runtimeServerManifest);
            if (!JarManifestUtils.isUnknown(clientServerVersion) && !JarManifestUtils.isUnknown(runtimeServerVersion)) {
                matchesVersion = clientServerVersion.equals(runtimeServerVersion);
            }

            PluginLog.info("========= Files ========");
            PluginLog.info(" - client installation > " + clientServerJarFile.getAbsolutePath());
            PluginLog.info(" -  'HytaleServer.jar' > " + clientServerJarFile.getAbsolutePath());
            PluginLog.info(" -  'HytaleServer.aot' > " + clientAOTFile.getAbsolutePath());
            PluginLog.info(" -        'Assets.zip' > " + clientAssetsFile.getAbsolutePath());
            PluginLog.info(" -           Patchline > " + pathlineName);
            PluginLog.info("======= Sources ========");
            PluginLog.info(" -    Includes assets? > " + booleanToHuman(hasAnyResources));
            PluginLog.info(" -   Found main class? > " + booleanToHuman(hasMainClass));
            PluginLog.info(" - JAR file build task > \"" + archiveTaskName + "\"");
            PluginLog.info(" - JAR file build name > " + archiveFile.getName());
            PluginLog.info(" - JAR file build path > " + archiveFile.getAbsolutePath());
            PluginLog.info("======= Runtime ========");
            PluginLog.info(" -           Directory > " + infoRuntimeDirectory);
            PluginLog.info(" -        Is runnable? > " + booleanToHuman(isServerRunnable));
            PluginLog.info("======= Versions =======");
            PluginLog.info(" -       Client-Server > " + clientServerVersion);
            PluginLog.info(" -      Runtime-Server > " + runtimeServerVersion);
            PluginLog.info(" -   Versions matching > " + booleanToHuman(matchesVersion));
        });
    }

    private String booleanToHuman(boolean value) {
        return value ? "YES" : "NO";
    }
}
