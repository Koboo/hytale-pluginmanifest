package eu.koboo.pluginmanifest;

import eu.koboo.pluginmanifest.model.AuthorInfo;
import groovy.json.JsonOutput;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class PluginManifestPlugin implements Plugin<Project> {

    public static final String EXTENSION_NAME = "pluginManifest";
    public static final String TASK_NAME = "generateManifestJson";

    @Override
    public void apply(Project target) {
        PluginManifestExtension extension = target.getExtensions()
                .create(EXTENSION_NAME, PluginManifestExtension.class);

        DirectoryProperty directoryProperty = target.getLayout().getBuildDirectory();
        File buildDirectory = directoryProperty.get().getAsFile();
        File resourceDirectory = new File(buildDirectory, "generated/resources");

        TaskProvider<PluginManifestTask> provider = target.getTasks().register(TASK_NAME, PluginManifestTask.class);

        target.afterEvaluate(project -> {
            SourceSet mainSourceSet = project.getExtensions()
                    .getByType(SourceSetContainer.class)
                    .getByName("main");
            mainSourceSet.getResources().srcDir(resourceDirectory);

            String manifestJson = buildJson(extension);

            provider.configure(task -> {
                task.plugin = this;
                task.setGroup("other");
                task.getOutputFile().set(new File(resourceDirectory, "manifest.json"));
                task.getManifestJson().set(manifestJson);
            });

            project.getTasks().named(mainSourceSet.getProcessResourcesTaskName(), task -> task.dependsOn(provider));
        });
    }

    private String buildJson(PluginManifestExtension extension) {
        Map<String, Object> manifestObject = new LinkedHashMap<>();

        manifestObject.put("Group", extension.getPluginGroup());
        manifestObject.put("Name", extension.getPluginName());
        manifestObject.put("Version", extension.getPluginVersion());
        String pluginDescription = extension.getPluginDescription();
        if(pluginDescription != null) {
            manifestObject.put("Description", pluginDescription);
        }

        List<Map<String, String>> authorList = new LinkedList<>();
        for (AuthorInfo author : extension.getAuthors()) {
            Map<String, String> authorObject = new LinkedHashMap<>();
            authorObject.put("Name", author.getName());
            String email = author.getEmail();
            if(email != null) {
                authorObject.put("Email", email);
            }
            String url = author.getUrl();
            if(url != null) {
                authorObject.put("Url", url);
            }
            authorList.add(authorObject);
        }
        manifestObject.put("Authors", authorList);

        String pluginWebsite = extension.getPluginWebsite();
        if(pluginWebsite != null) {
            manifestObject.put("Website", pluginWebsite);
        }

        manifestObject.put("ServerVersion", "*");

        Map<String, String> dependencies = extension.getDependencies();
        if(!dependencies.isEmpty()) {
            manifestObject.put("Dependencies", dependencies);
        }
        Map<String, String> optionalDependencies = extension.getOptionalDependencies();
        if(!optionalDependencies.isEmpty()) {
            manifestObject.put("OptionalDependencies", optionalDependencies);
        }
        if(!extension.getLoadBefore().isEmpty()) {
            manifestObject.put("LoadBefore", extension.getLoadBefore());
        }
        if(extension.isDisabledByDefault()) {
            manifestObject.put("DisabledByDefault", true);
        }
        if(extension.isIncludesAssetPack()) {
            manifestObject.put("IncludesAssetPack", true);
        }

        manifestObject.put("Main", extension.getPluginMain());

        String json = JsonOutput.toJson(manifestObject);

        boolean prettyPrint = true;
        if(prettyPrint) {
            json = JsonOutput.prettyPrint(json);
        }
        return json;
    }
}
