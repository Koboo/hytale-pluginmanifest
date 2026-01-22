package eu.koboo.pluginmanifest.gradle.plugin;

import eu.koboo.pluginmanifest.gradle.plugin.extension.clientinstall.ClientInstallationExtension;
import eu.koboo.pluginmanifest.gradle.plugin.extension.manifest.JsonManifestExtension;
import eu.koboo.pluginmanifest.gradle.plugin.extension.serverruntime.ServerRuntimeExtension;
import eu.koboo.pluginmanifest.gradle.plugin.tasks.GenerateManifestTask;
import eu.koboo.pluginmanifest.gradle.plugin.tasks.RunServerTask;
import eu.koboo.pluginmanifest.gradle.plugin.utils.JavaSourceUtils;
import eu.koboo.pluginmanifest.gradle.plugin.utils.ProviderUtils;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
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

import java.io.File;
import java.util.Locale;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class PluginManifestPlugin implements Plugin<Project> {

    private static final String EXTENSION_NAME = "pluginManifest";
    private static final String TASK_GROUP_NAME = EXTENSION_NAME.toLowerCase(Locale.ROOT);

    private static final String GENERATE_MANIFEST = "generateManifest";
    private static final String RUN_SERVER = "runServer";

    public static final String RESOURCE_DIRECTORY = "generated" + File.separator + "pluginmanifest";
    public static final String MANIFEST = "manifest.json";

    @Override
    public void apply(Project target) {
        PluginManifestExtension extension = target.getExtensions().create(EXTENSION_NAME, PluginManifestExtension.class);

        JsonManifestExtension manifestExt = extension.jsonManifestExtension;
        applyDefaults(target, manifestExt);

        ServerRuntimeExtension runtimeExt = extension.serverRuntimeExtension;
        ClientInstallationExtension installExt = extension.installationExtension;

        TaskProvider<GenerateManifestTask> generateManifestProvider = target.getTasks().register(GENERATE_MANIFEST, GenerateManifestTask.class);
        TaskProvider<RunServerTask> runServerProvider = target.getTasks().register(RUN_SERVER, RunServerTask.class);

        target.afterEvaluate(project -> {

            // Resolve the client installation directory
            File clientServerJarFile = installExt.resolveClientServerJarFile();
            if (!clientServerJarFile.exists()) {
                throw new GradleException("Can't find server jar file at " + clientServerJarFile.getAbsolutePath());
            }

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
            Provider<Directory> genResourceDir = project.getLayout()
                .getBuildDirectory()
                .dir(RESOURCE_DIRECTORY);
            mainSourceSet.getResources().srcDir(genResourceDir);

            Jar archiveTask = JavaSourceUtils.resolveArchiveTask(project);
            Provider<RegularFile> archiveFileProvider = archiveTask.getArchiveFile();

            //
            // ==== "generateManifestJson" ====
            //
            generateManifestProvider.configure(task -> {
                task.setGroup(TASK_GROUP_NAME);
                task.setDescription("Generates the manifest.json and puts into plugins jar file.");
                task.getResourceDirectory().set(genResourceDir);
                task.getManifestMap().set(ProviderUtils.createManifestProvider(project));
                task.getMinimizeJson().set(manifestExt.getMinimizeJson());
            });

            //
            // ==== "runServer" ====
            //
            runServerProvider.configure(task -> {
                task.setGroup(TASK_GROUP_NAME);
                task.setDescription("Runs the server in your server directory with console support in the terminal.");

                task.getClientServerJarFile().set(installExt.resolveClientServerJarFile());
                task.getClientAOTFile().set(installExt.resolveClientAOTFile());
                task.getClientAssetsFile().set(installExt.resolveClientAssetsFile());
                task.getArchiveFile().set(archiveFileProvider);

                task.getRuntimeDirectory().set(runtimeExt.resolveRuntimeDirectory());
                task.getCopyPluginToRuntime().set(runtimeExt.getCopyPluginToRuntime());

                task.getAllowOp().set(runtimeExt.getAllowOp());
                task.getUserJvmArguments().set(runtimeExt.getJvmArguments());
                task.getUserServerArguments().set(runtimeExt.getServerArguments());
            });

            //
            // ==== INFORMATION PRINTING ====
            //

            PluginDoctor.printDoctor(project, runtimeExt, installExt);
        });
    }

    private void applyDefaults(Project project, JsonManifestExtension manifestExt) {
        manifestExt.getPluginGroup().convention(ProviderUtils.createPluginGroupProvider(project));
        manifestExt.getPluginName().convention(ProviderUtils.createPluginNameProvider(project));
        manifestExt.getPluginVersion().convention(ProviderUtils.createPluginVersionProvider(project));
        manifestExt.getPluginMainClass().convention(ProviderUtils.createPluginMainClassCandidateProvider(project));
        manifestExt.getServerVersion().convention("*");
        manifestExt.getDisabledByDefault().convention(false);
        manifestExt.getIncludesAssetPack().convention(ProviderUtils.createHasResourcesProvider(project));
        manifestExt.getMinimizeJson().convention(false);
    }
}
