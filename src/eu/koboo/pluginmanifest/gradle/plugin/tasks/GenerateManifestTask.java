package eu.koboo.pluginmanifest.gradle.plugin.tasks;

import eu.koboo.pluginmanifest.gradle.plugin.PluginManifestPlugin;
import eu.koboo.pluginmanifest.gradle.plugin.utils.PluginLog;
import groovy.json.JsonOutput;
import org.gradle.api.DefaultTask;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

public abstract class GenerateManifestTask extends DefaultTask {

    @OutputDirectory
    public abstract DirectoryProperty getResourceDirectory();

    @Input
    public abstract MapProperty<String, Object> getManifestMap();

    @TaskAction
    public void runTask() throws IOException {

        Map<String, Object> manifestMap = getManifestMap().get();
        String manifestJson = JsonOutput.toJson(manifestMap);
        manifestJson = JsonOutput.prettyPrint(manifestJson);

        Directory directory = getResourceDirectory().getOrNull();
        if (directory == null) {
            throw new InvalidUserDataException("Can't resolve resourceDirectory, because it can't be null!");
        }
        File resourceDirectory = directory.getAsFile();
        if (!resourceDirectory.exists()) {
            resourceDirectory.mkdirs();
        }
        File manifestFile = new File(resourceDirectory, PluginManifestPlugin.MANIFEST);

        Files.writeString(manifestFile.toPath(), manifestJson, StandardCharsets.UTF_8);

        PluginLog.info("");
        PluginLog.info("Successfully generated manifest.json at:");
        PluginLog.info(manifestFile.getAbsolutePath());
        PluginLog.info("");
    }
}
