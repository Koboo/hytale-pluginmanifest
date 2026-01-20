package eu.koboo.pluginmanifest.gradle.plugin;

import eu.koboo.pluginmanifest.gradle.plugin.extension.clientinstall.ClientInstallationExtension;
import eu.koboo.pluginmanifest.gradle.plugin.extension.manifest.JsonManifestExtension;
import eu.koboo.pluginmanifest.gradle.plugin.extension.serverruntime.ServerRuntimeExtension;
import eu.koboo.pluginmanifest.gradle.plugin.tasks.GenerateManifestTask;
import eu.koboo.pluginmanifest.gradle.plugin.tasks.InstallPluginTask;
import eu.koboo.pluginmanifest.gradle.plugin.tasks.RunServerTask;
import eu.koboo.pluginmanifest.gradle.plugin.tasks.UpdateServerTask;
import eu.koboo.pluginmanifest.gradle.plugin.utils.JarManifestUtils;
import eu.koboo.pluginmanifest.gradle.plugin.utils.JavaSourceUtils;
import eu.koboo.pluginmanifest.gradle.plugin.utils.PluginLog;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DuplicatesStrategy;
import org.gradle.api.file.RegularFile;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.jvm.tasks.Jar;
import org.gradle.language.jvm.tasks.ProcessResources;
import org.jetbrains.java.decompiler.api.Decompiler;
import org.jetbrains.java.decompiler.main.decompiler.SingleFileSaver;

import java.io.File;
import java.util.Locale;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class PluginManifestPlugin implements Plugin<Project> {

    private static final String EXTENSION_NAME = "pluginManifest";
    private static final String TASK_GROUP_NAME = EXTENSION_NAME.toLowerCase(Locale.ROOT);

    private static final String GENERATE_MANIFEST = "generateManifest";
    private static final String UPDATE_SERVER = "updateServer";
    private static final String RUN_SERVER = "runServer";
    private static final String INSTALL_PLUGIN = "installPlugin";
    private static final String BUILD_AND_RUN = "buildAndRun";

    private static final String ONLY_DECOMPILE_PACKAGE = "com/hypixel";
    private static final String TASK_PROCESS_RESOURCES = "processResources";

    public static final String RESOURCE_DIRECTORY = "generated/pluginmanifest";
    public static final String MANIFEST = "manifest.json";

    @Override
    public void apply(Project target) {
        PluginManifestExtension extension = target.getExtensions().create(EXTENSION_NAME, PluginManifestExtension.class);
        TaskProvider<GenerateManifestTask> generateManifestProvider = target.getTasks().register(GENERATE_MANIFEST, GenerateManifestTask.class);
        TaskProvider<UpdateServerTask> updateServerProvider = target.getTasks().register(UPDATE_SERVER, UpdateServerTask.class);
        TaskProvider<RunServerTask> runServerProvider = target.getTasks().register(RUN_SERVER, RunServerTask.class);
        TaskProvider<InstallPluginTask> installPluginProvider = target.getTasks().register(INSTALL_PLUGIN, InstallPluginTask.class);

        target.getTasks().register(BUILD_AND_RUN, Task.class, task -> {
            task.setGroup(TASK_GROUP_NAME);
            task.setDescription("Builds your plugin, copies the jar into the mods folder and starts the server.");
            task.dependsOn(
                target.getProject().getTasks().getByName(TASK_PROCESS_RESOURCES),
                installPluginProvider,
                runServerProvider
            );
        });

        target.afterEvaluate(project -> {
            JsonManifestExtension manifestExt = extension.jsonManifestExtension;
            ServerRuntimeExtension runtimeExt = extension.serverRuntimeExtension;
            ClientInstallationExtension installExt = extension.installationExtension;

            // Resolve the client installation directory
            File clientServerJarFile = installExt.resolveClientServerJarFile();
            if (!clientServerJarFile.exists()) {
                throw new GradleException("Can't find server jar file at " + clientServerJarFile.getAbsolutePath());
            }
            decompileServerSource(installExt);

            // Applying server dependency as a file.
            project.getRepositories().flatDir(repository ->
                repository.dirs(installExt.resolveClientServerDirectory())
            );
            project.getDependencies().add(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME, ":HytaleServer");

            // Depend "processResources" on "generateManifest"
            // Exclude/Override actual "src/resources/manifest.json"
            target.getTasks().withType(ProcessResources.class).configureEach(task -> {
                task.dependsOn(generateManifestProvider);
                task.doLast(t -> {
                    t.getOutputs().getFiles().forEach(outputFile -> {
                        if (!outputFile.getName().endsWith(MANIFEST)) {
                            return;
                        }
                        task.setDuplicatesStrategy(DuplicatesStrategy.INCLUDE);
                    });
                });
            });

            // Adding PROJECT/build/generated/pluginmanifest/ to sourceSet resources.
            SourceSet mainSourceSet = project.getExtensions()
                .getByType(SourceSetContainer.class)
                .getByName("main");
            Provider<Directory> genResourceDir = project.getLayout().getBuildDirectory().dir(RESOURCE_DIRECTORY);
            mainSourceSet.getResources().srcDir(genResourceDir);

            // Configure "generateManifestJson"
            generateManifestProvider.configure(task -> {
                task.setGroup(TASK_GROUP_NAME);
                task.setDescription("Generates the manifest.json and puts into plugins jar file.");
                task.getResourceDirectory().set(genResourceDir);
                task.getProjectGroupId().set(project.getGroup().toString());
                task.getProjectArtifactId().set(project.getName());
                task.getProjectVersion().set(project.getVersion().toString());
                task.getHasAnyResources().set(JavaSourceUtils.hasAnyResource(mainSourceSet));
                task.getMainClassCandidates().set(JavaSourceUtils.getMainClassCandidates(mainSourceSet));
                task.getExtension().set(manifestExt);
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
                task.setDescription("Runs the server in your server directory with console support in the terminal.");
                task.getRuntimeExtension().set(runtimeExt);
            });

            // Configure "installPlugin"
            installPluginProvider.configure(task -> {
                Jar archiveTask = JavaSourceUtils.resolveArchiveTask(project);
                Provider<RegularFile> archiveFileProvider = archiveTask.getArchiveFile();

                task.setGroup(TASK_GROUP_NAME);
                task.setDescription("Execute the jar archive task and moves the plugin into your server's \"/mods\" directory.");
                task.getRuntimeExtension().set(runtimeExt);
                task.getArchiveFilePath().set(archiveFileProvider);
                task.dependsOn(archiveTask);
            });

            //
            // ==== INFORMATION PRINTING ====
            //

            PluginDoctor.printDoctor(project, runtimeExt, installExt);
        });
    }

    private void decompileServerSource(ClientInstallationExtension installExt) {
        if (!installExt.getDecompileServer().get()) {
            PluginLog.info("Decompiling server sources is disabled.");
            return;
        }
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
            if (jarVersion.equals(sourcesVersion)) {
                return;
            }
            PluginLog.info("Sources file is outdated!");
            clientServerSourcesFile.delete();
            PluginLog.info("Deleted previous sources file.");
        }
        PluginLog.info("Decompiling server sources...");

        Decompiler decompiler = new Decompiler.Builder()
            .inputs(clientServerJarFile)
            .output(new SingleFileSaver(clientServerSourcesFile))
            .option("only", ONLY_DECOMPILE_PACKAGE)
            .option("simplify-switch", "1")
            .option("decompile-generics", "1")
            .option("remove-synthetic", "0")
            .option("remove-bridge", "0")
            .option("hide-empty-super", "1")
            .option("hide-default-constructor", "1")
            .option("remove-empty-try-catch", "1")
            .build();

        PluginLog.info("Decompiling server sources.. ");
        PluginLog.info("Please wait..");
        PluginLog.info("This may take a few minutes..");
        decompiler.decompile();

        PluginLog.info("");
        PluginLog.info("Decompiled server sources.");
        PluginLog.info(clientServerSourcesFile.getAbsolutePath());
        PluginLog.info("");
    }
}
