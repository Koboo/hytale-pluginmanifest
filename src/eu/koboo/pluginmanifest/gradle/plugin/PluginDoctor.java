package eu.koboo.pluginmanifest.gradle.plugin;

import eu.koboo.pluginmanifest.gradle.plugin.extension.ClientFiles;
import eu.koboo.pluginmanifest.gradle.plugin.extension.clientinstall.ClientInstallationExtension;
import eu.koboo.pluginmanifest.gradle.plugin.extension.serverruntime.ServerRuntimeExtension;
import eu.koboo.pluginmanifest.gradle.plugin.utils.JarManifestUtils;
import eu.koboo.pluginmanifest.gradle.plugin.utils.JavaSourceUtils;
import eu.koboo.pluginmanifest.gradle.plugin.utils.PluginLog;
import eu.koboo.pluginmanifest.gradle.plugin.utils.ProviderUtils;
import groovy.json.JsonOutput;
import lombok.experimental.UtilityClass;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Project;
import org.gradle.jvm.tasks.Jar;

import java.io.File;
import java.util.Map;

@UtilityClass
public class PluginDoctor {

    private static final String FOUND = "[✓] Found";
    private static final String NOT_FOUND = "[ ] Not found";

    public void printDoctor(Project project) {

        PluginManifestExtension extension = project.getExtensions().getByType(PluginManifestExtension.class);
        ServerRuntimeExtension runtimeExt = extension.getServerRuntimeExtension();
        ClientInstallationExtension installExt = extension.getInstallationExtension();

        File clientInstallDirectory = new File(installExt.getClientInstallDirectory().get());
        File clientServerJarFile = installExt.provideClientFile(ClientFiles.SERVER_JAR).get().getAsFile();
        File clientAssetsFile = installExt.provideClientFile(ClientFiles.ASSETS_ZIP).get().getAsFile();
        File clientSourcesFile = installExt.provideClientFile(ClientFiles.SOURCES_JAR).get().getAsFile();
        File clientAOTFile = installExt.provideClientFile(ClientFiles.AOT_FILE).get().getAsFile();

        String clientServerVersion = JarManifestUtils.getVersion(clientServerJarFile);

        String runtimeDirectoryPath = runtimeExt.getRuntimeDirectory().getOrNull();
        File runtimeDirectory = null;
        String runtimeText = "Not configured";
        if (runtimeDirectoryPath != null && !runtimeDirectoryPath.trim().isEmpty()) {
            runtimeDirectory = runtimeExt.provideRuntimeDirectory(project);
            if (runtimeDirectory.exists() && !runtimeDirectory.isDirectory()) {
                throw new InvalidUserDataException("Configured runtimeDirectory is not a directory!");
            }
            if (!runtimeDirectory.exists()) {
                runtimeDirectory.mkdirs();
            }
            runtimeText = runtimeDirectory.getAbsolutePath();
        }

        String serverJarText = "Not configured";
        File serverJarFile = null;
        if (runtimeDirectory != null && runtimeDirectory.exists() && runtimeDirectory.isDirectory()) {
            File runtimeServerJar = new File(runtimeDirectory, "HytaleServer.jar");
            if (runtimeServerJar.exists() && runtimeServerJar.isFile()) {
                serverJarFile = runtimeServerJar;
                serverJarText = "From runtimeDirectory";
            } else {
                serverJarFile = clientServerJarFile;
                serverJarText = "From client installation";
            }
        }
        String runnableText = "NO";
        if (serverJarFile != null && serverJarFile.exists() && serverJarFile.isFile()) {
            runnableText = "YES";
        }

        // Parse versions by MANIFEST of client and runtime server jar
        String runtimeServerVersion = JarManifestUtils.getVersion(serverJarFile);
        String matchesVersion = "NO";
        if (!JarManifestUtils.isUnknown(clientServerVersion) && !JarManifestUtils.isUnknown(runtimeServerVersion)) {
            if (clientServerVersion.equals(runtimeServerVersion)) {
                matchesVersion = "YES";
            }
        } else {
            matchesVersion = "Both unknown";
        }

        Jar archiveTask = JavaSourceUtils.resolveArchiveTask(project);
        File archiveFile = archiveTask.getArchiveFile().get().getAsFile();
        String archiveTaskName = archiveTask.getName();

        Map<String, Object> manifestMap = ProviderUtils.createManifestProvider(project).get();
        String manifestJson = JsonOutput.toJson(manifestMap);
        manifestJson = JsonOutput.prettyPrint(manifestJson);

        String patchlineName = installExt.resolvePatchlineProvider().get();

        PluginLog.info("PluginManifest doctor results for \"" + project.getName() + "\":");
        PluginLog.print("========= Client Installation ========");
        PluginLog.print("");
        PluginLog.print("                      Path > " + clientInstallDirectory.getAbsolutePath());
        PluginLog.print("                 Patchline > " + patchlineName);
        PluginLog.print("            Server-Version > " + JarManifestUtils.getVersion(clientServerJarFile));
        PluginLog.print("        'HytaleServer.jar' > " + fileExists(clientServerJarFile));
        PluginLog.print("        'HytaleServer.aot' > " + fileExists(clientAOTFile));
        PluginLog.print("'HytaleServer-sources.jar' > " + fileExists(clientSourcesFile));
        PluginLog.print("              'Assets.zip' > " + fileExists(clientAssetsFile));
        PluginLog.print("");
        PluginLog.print("============== Manifest ==============");
        if (extension.getDisableManifestGeneration().get()) {
            PluginLog.print("");
            PluginLog.print("Manifest generation is disabled.");
            PluginLog.print("");
        } else {
            PluginLog.print(manifestJson);
        }
        PluginLog.print("============== JAR file ==============");
        PluginLog.print("");
        PluginLog.print(" JAR file build task > \"" + archiveTaskName + "\"");
        PluginLog.print(" JAR file build name > " + archiveFile.getName());
        PluginLog.print(" JAR file build path > " + archiveFile.getAbsolutePath());
        PluginLog.print("");
        PluginLog.print("=============== Runtime ==============");
        PluginLog.print("");
        PluginLog.print("  Server-Runtime-Directory > " + runtimeText);
        PluginLog.print("    Is runtime executable? > " + runnableText);
        PluginLog.print("   Which 'HytaleServer.jar'> " + serverJarText);
        PluginLog.print("            Server-Version > " + runtimeServerVersion);
        PluginLog.print("   Version matches client? > " + matchesVersion);
        PluginLog.print("");
        PluginLog.print("======================================");
    }

    private String fileExists(File file) {
        if (file == null) {
            return NOT_FOUND;
        }
        return file.exists() ? FOUND : NOT_FOUND;
    }
}
