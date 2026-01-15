package eu.koboo.pluginmanifest.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public abstract class PluginManifestTask extends DefaultTask {

    @OutputFile
    public abstract RegularFileProperty getOutputFile();

    @Input
    public abstract Property<String> getManifestJson();

    @TaskAction
    public void runTask() throws IOException {
        String manifestJson = getManifestJson().get();

        File outputFile = getOutputFile().get().getAsFile();
        File parentDir = outputFile.getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }

        Files.writeString(outputFile.toPath(), manifestJson, StandardCharsets.UTF_8);
        log("Generated at: " + outputFile.getAbsolutePath());
    }

    private void log(String message) {
        getLogger().lifecycle(PluginManifestPlugin.LOG_PREFIX + PluginManifestPlugin.TASK_NAME + ": " + message);
    }
}
