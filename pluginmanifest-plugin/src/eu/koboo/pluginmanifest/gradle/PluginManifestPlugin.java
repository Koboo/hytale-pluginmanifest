package eu.koboo.pluginmanifest.gradle;

import eu.koboo.pluginmanifest.gradle.configs.PluginManifestExtension;
import eu.koboo.pluginmanifest.manifest.ManifestFile;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.file.RegularFile;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.language.jvm.tasks.ProcessResources;

import java.io.File;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class PluginManifestPlugin implements Plugin<Project> {

    public static final String LOG_PREFIX = "> PluginManifest :";
    public static final String EXTENSION_NAME = "pluginManifest";
    public static final String TASK_NAME = "generateManifestJson";
    public static final String BUILD_DIRECTORY = "generated/pluginmanifest/";

    public static final String SERVER_JAR_NAME = "HytaleServer.jar";
    public static final String LIBS_DIRECTORY = "libs/";
    public static final String CLIENT_SERVER_PATH = "Hytale/install/release/package/game/latest/Server/";


    @Override
    public void apply(Project target) {
        PluginManifestExtension extension = target.getExtensions().create(EXTENSION_NAME, PluginManifestExtension.class);
        TaskProvider<PluginManifestTask> provider = target.getTasks().register(TASK_NAME, PluginManifestTask.class);

        target.afterEvaluate(project -> {
            applyResourceDirectory(project);
            configureTaskProvider(project, provider, extension);
            configureResourceTasks(project, provider);
            applyServerDependency(project, extension);
        });
    }

    public void log(Project project, String method, String message) {
        project.getLogger().lifecycle(LOG_PREFIX + method + ":" + message);
    }

    private void applyResourceDirectory(Project project) {
        File resourceDirectory = GradleUtils.getDirectoryInBuild(project, BUILD_DIRECTORY);
        SourceSet mainSourceSet = GradleUtils.getMainSourceSet(project);
        mainSourceSet.getResources().srcDir(resourceDirectory);
    }

    private void configureTaskProvider(Project project,
                                       TaskProvider<PluginManifestTask> provider,
                                       PluginManifestExtension extension) {

        ManifestUtils.applyAutomaticProperties(this, project, extension);
        String manifestJson = ManifestUtils.buildJson(extension);

        File resourceDirectory = GradleUtils.getDirectoryInBuild(project, BUILD_DIRECTORY);
        provider.configure(task -> {
            task.setGroup("other");
            task.getOutputFile().set(new File(resourceDirectory, ManifestFile.NAME));
            task.getManifestJson().set(manifestJson);
        });
    }

    private void configureResourceTasks(Project project, TaskProvider<PluginManifestTask> provider) {
        project.getTasks()
            .withType(ProcessResources.class)
            .configureEach(task -> {
                task.dependsOn(provider);
                task.exclude(ManifestFile.NAME);
            });
    }

    private void applyServerDependency(Project project, PluginManifestExtension extension) {
        if(!extension.isApplyServerDependency()) {
            log(project, "applyServerDependency", "Applying server dependency is disabled.");
            return;
        }
        final File finalServerJarFile = DependencyUtils.searchServerFile(this, project, extension);
        if(finalServerJarFile == null) {
            // Errors are printed within searchServerFile.
            return;
        }
        Provider<RegularFile> jarProvider = project.getLayout().file(project.provider(() -> finalServerJarFile));
        DependencyHandler dependencies = project.getDependencies();
        dependencies.add(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME, project.files(jarProvider));
        log(project, "applyServerDependency", "Dependency added from " + finalServerJarFile.getAbsolutePath());
    }
}
