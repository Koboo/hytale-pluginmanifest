package eu.koboo.pluginmanifest.gradle.plugin;

import eu.koboo.pluginmanifest.gradle.plugin.extension.ClientFiles;
import eu.koboo.pluginmanifest.gradle.plugin.extension.Patchline;
import eu.koboo.pluginmanifest.gradle.plugin.extension.clientinstall.ClientInstallationExtension;
import eu.koboo.pluginmanifest.gradle.plugin.extension.manifest.JsonManifestExtension;
import eu.koboo.pluginmanifest.gradle.plugin.extension.serverruntime.ServerRuntimeExtension;
import eu.koboo.pluginmanifest.gradle.plugin.tasks.DecompileServerTask;
import eu.koboo.pluginmanifest.gradle.plugin.tasks.GenerateManifestTask;
import eu.koboo.pluginmanifest.gradle.plugin.tasks.RunServerTask;
import eu.koboo.pluginmanifest.gradle.plugin.utils.JavaSourceUtils;
import eu.koboo.pluginmanifest.gradle.plugin.utils.ProviderUtils;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.Directory;
import org.gradle.api.file.RegularFile;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.jvm.tasks.Jar;

import java.io.File;
import java.util.LinkedList;
import java.util.Locale;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class PluginManifestPlugin implements Plugin<Project> {

    private static final String EXTENSION_NAME = "pluginManifest";
    private static final String TASK_GROUP_NAME = EXTENSION_NAME.toLowerCase(Locale.ROOT);

    private static final String GENERATE_MANIFEST = "generateManifest";
    private static final String RUN_SERVER = "runServer";
    private static final String DECOMPILE_SERVER = "decompileServer";

    public static final String RESOURCE_DIRECTORY = "generated" + File.separator + "pluginmanifest";
    public static final String MANIFEST = "manifest.json";

    @Override
    public void apply(Project target) {
        PluginManifestExtension extension = target.getExtensions().create(EXTENSION_NAME, PluginManifestExtension.class);

        JsonManifestExtension manifestExt = extension.jsonManifestExtension;
        applyManifestDefaults(target, manifestExt);

        ServerRuntimeExtension runtimeExt = extension.serverRuntimeExtension;
        applyRuntimeDefault(target, runtimeExt);

        ClientInstallationExtension installExt = extension.installationExtension;
        applyInstallDefaults(target, installExt);

        TaskProvider<GenerateManifestTask> generateManifestProvider = target.getTasks().register(GENERATE_MANIFEST, GenerateManifestTask.class);
        TaskProvider<RunServerTask> runServerProvider = target.getTasks().register(RUN_SERVER, RunServerTask.class);
        TaskProvider<DecompileServerTask> decompileServerProvider = target.getTasks().register(DECOMPILE_SERVER, DecompileServerTask.class);

        target.afterEvaluate(project -> {

            // Applying server dependency as a file.
            if (extension.getAddClientServerDependency().get()) {
                project.getRepositories().flatDir(repository ->
                    repository.dirs(installExt.provideClientDirectory(ClientFiles.SERVER_DIR))
                );
                project.getDependencies().add(
                    JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME,
                    ":HytaleServer"
                );
            }

            // Adding default maven repositories
            if (extension.getAddDefaultRepositories().get()) {
                project.getRepositories().mavenLocal();
                project.getRepositories().mavenCentral();
                project.getRepositories().maven(repository -> {
                    repository.setName("hytale-release");
                    repository.setUrl("https://maven.hytale.com/release");
                });
                project.getRepositories().maven(repository -> {
                    repository.setName("hytale-pre-release");
                    repository.setUrl("https://maven.hytale.com/pre-release");
                });
            }

            // Adding PROJECT/build/generated/pluginmanifest/ to sourceSet resources.
            Provider<Directory> generatedResourceDir = project.getLayout()
                .getBuildDirectory()
                .dir(RESOURCE_DIRECTORY);
            if (!extension.getDisableManifestGeneration().get()) {
                SourceSet mainSourceSet = project.getExtensions()
                    .getByType(SourceSetContainer.class)
                    .getByName("main");
                mainSourceSet.getResources().srcDir(generatedResourceDir);
            }

            Jar archiveTask = JavaSourceUtils.resolveArchiveTask(project);
            Provider<RegularFile> archiveFileProvider = archiveTask.getArchiveFile();

            //
            // ==== "generateManifestJson" ====
            //
            generateManifestProvider.configure(task -> {
                task.setGroup(TASK_GROUP_NAME);
                task.setDescription("Generates the manifest.json and puts into plugins jar file.");
                task.getResourceDirectory().set(generatedResourceDir);
                task.getManifestMap().set(ProviderUtils.createManifestProvider(project));
            });
            // Create task dependencies for "generateManifest"
            if (!extension.getDisableManifestGeneration().get()) {
                Task processResources = target.getTasks().getByName("processResources");
                processResources.dependsOn(generateManifestProvider);
                Task javadocJar = target.getTasks().getByName("javadocJar");
                javadocJar.dependsOn(generateManifestProvider);
                Task sourcesJar = target.getTasks().getByName("sourcesJar");
                sourcesJar.dependsOn(generateManifestProvider);
            }

            //
            // ==== "runServer" ====
            //
            runServerProvider.configure(task -> {
                task.setGroup(TASK_GROUP_NAME);
                task.setDescription("Runs the server in your server directory with console support in the terminal.");

                task.getClientServerJarFile().set(installExt.provideClientFile(ClientFiles.SERVER_JAR));
                task.getClientAOTFile().set(installExt.provideClientFile(ClientFiles.AOT_FILE));
                task.getClientAssetsFile().set(installExt.provideClientFile(ClientFiles.ASSETS_ZIP));
                task.getArchiveFile().set(archiveFileProvider);

                task.getRuntimeDirectory().set(runtimeExt.provideRuntimeDirectory(project));
                task.getCopyPluginToRuntime().set(runtimeExt.getCopyPluginToRuntime());
                task.getDeleteLogsOnStart().set(runtimeExt.getDeleteLogsOnStart());

                task.getAllowOp().set(runtimeExt.getAllowOp());
                task.getUserJvmArguments().set(runtimeExt.getJvmArguments());
                task.getUserServerArguments().set(runtimeExt.getServerArguments());
            });

            //
            // ==== "decompileServer" ====
            //
            decompileServerProvider.configure(task -> {
                task.setGroup(TASK_GROUP_NAME);
                task.setDescription("Decompiles the server sources from and into the client installation path");
                task.getClientServerJarFile().set(installExt.provideClientFile(ClientFiles.SERVER_JAR));
                task.getClientSourcesJarFile().set(installExt.provideClientFile(ClientFiles.SOURCES_JAR));
                task.getVineflowerJarFile().set(installExt.provideClientFile(ClientFiles.VINEFLOWER_JAR));
            });

            //
            // ==== INFORMATION PRINTING ====
            //

            PluginDoctor.printDoctor(project);
        });
    }

    private void applyManifestDefaults(Project project, JsonManifestExtension manifestExt) {
        manifestExt.getPluginGroup().convention(ProviderUtils.createPluginGroupProvider(project));
        manifestExt.getPluginName().convention(ProviderUtils.createPluginNameProvider(project));
        manifestExt.getPluginVersion().convention(ProviderUtils.createPluginVersionProvider(project));
        manifestExt.getPluginMainClass().convention(ProviderUtils.createPluginMainClassCandidateProvider(project));
        manifestExt.getServerVersion().convention("*");
        manifestExt.getDisabledByDefault().convention(false);
        manifestExt.getIncludesAssetPack().convention(ProviderUtils.createHasResourcesProvider(project));
    }

    private void applyRuntimeDefault(Project project, ServerRuntimeExtension runtimeExtension) {
        runtimeExtension.getIsProjectRelative().convention(true);
        runtimeExtension.getCopyPluginToRuntime().convention(false);
        runtimeExtension.getDeleteLogsOnStart().convention(true);

        runtimeExtension.getAllowOp().convention(true);
        runtimeExtension.getBindAddress().convention("0.0.0.0:5520");
        runtimeExtension.getJvmArguments().convention(new LinkedList<>());
        runtimeExtension.getServerArguments().convention(new LinkedList<>());
    }

    private void applyInstallDefaults(Project project, ClientInstallationExtension installExt) {
        installExt.getPatchline().convention(Patchline.RELEASE);
        installExt.getClientInstallDirectory().convention(installExt.createDefaultAppDataProvider());
    }
}
