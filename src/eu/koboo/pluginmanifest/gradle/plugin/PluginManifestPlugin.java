package eu.koboo.pluginmanifest.gradle.plugin;

import eu.koboo.pluginmanifest.gradle.plugin.extension.PluginManifestExtension;
import eu.koboo.pluginmanifest.gradle.plugin.extension.manifest.ManifestExtension;
import eu.koboo.pluginmanifest.gradle.plugin.extension.serverdependency.ServerRuntimeExtension;
import eu.koboo.pluginmanifest.gradle.plugin.tasks.*;
import eu.koboo.pluginmanifest.gradle.plugin.utils.JavaSourceUtils;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.file.DuplicatesStrategy;
import org.gradle.api.file.RegularFile;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.language.jvm.tasks.ProcessResources;

import java.io.File;
import java.util.List;
import java.util.Locale;

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
            ManifestExtension manifestExt = extension.getManifestExtension();
            ServerRuntimeExtension runtimeExt = extension.getRuntimeExtension();

            // Resolve client installation directory
            File clientServerJarFile = runtimeExt.resolveClientServerJarFile();
            if (!clientServerJarFile.exists()) {
                throw new GradleException("Can't find server jar file at " + clientServerJarFile.getAbsolutePath());
            }

            File archiveFile = JavaSourceUtils.resolveArchive(project);
            String archiveTaskName = JavaSourceUtils.resolveArchiveTaskName(project);

            File clientAssetsFile = runtimeExt.resolveClientAssetsFile();
            File clientAOTFile = runtimeExt.resolveClientAOTFile();

            // Applying server dependency as a file.
            boolean applyServerDependency = runtimeExt.getApplyServerDependency().getOrElse(true);
            if (applyServerDependency) {
                Provider<RegularFile> jarProvider = project.getLayout().file(project.provider(() -> clientServerJarFile));
                DependencyHandler dependencies = project.getDependencies();
                dependencies.add(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME, project.files(jarProvider));
            }

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
            boolean hasMainClass = false;
            if (mainClassCandidates.size() == 1) {
                hasMainClass = true;
            }

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
            });

            // Configure "runServer"
            runServerProvider.configure(task -> {
                task.setGroup(TASK_GROUP_NAME);
                task.setDescription("Runs the server in your server directory with console support.");
                task.getRuntimeExtension().set(runtimeExt);
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

            String runtimeDirectory = "Not configured";
            String runtimeDirectoryPath = runtimeExt.getRuntimeDirectory().getOrNull();
            if (runtimeDirectoryPath != null && !runtimeDirectoryPath.trim().isEmpty()) {
                runtimeDirectory = new File(runtimeDirectoryPath).getAbsolutePath();
            }
            String pathlineName = runtimeExt.getPatchline().get().name().toLowerCase(Locale.ROOT);

            PluginLog.info("Resolved files:");
            PluginLog.info(" - 'HytaleServer.jar':");
            PluginLog.info("   > " + clientServerJarFile.getAbsolutePath());
            PluginLog.info(" - 'HytaleServer.aot':");
            PluginLog.info("   > " + clientAOTFile.getAbsolutePath());
            PluginLog.info(" - 'Assets.zip':");
            PluginLog.info("   > " + clientAssetsFile.getAbsolutePath());
            PluginLog.info(" - Patchline: " + pathlineName);
            PluginLog.info("Sources:");
            PluginLog.info(" - applied dependency? " + booleanToHuman(applyServerDependency));
            PluginLog.info(" - found resources? " + booleanToHuman(hasAnyResources));
            PluginLog.info(" - found main class?: " + booleanToHuman(hasMainClass));
            PluginLog.info("JAR-File:");
            PluginLog.info(" - JAR file build task: " + archiveTaskName);
            PluginLog.info(" - JAR file build path: " + archiveFile.getAbsolutePath());
            PluginLog.info("Runtime:");
            PluginLog.info(" - Server-Directory: " + runtimeDirectory);
        });
    }

    private String booleanToHuman(boolean value) {
        return value ? "yes" : "no";
    }
}
