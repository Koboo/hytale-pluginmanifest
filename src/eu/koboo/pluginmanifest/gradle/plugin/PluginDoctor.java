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
import java.util.List;
import java.util.Map;

@UtilityClass
public class PluginDoctor {

    private static final String FOUND = "[✓] Found";
    private static final String NOT_FOUND = "[ ] Not found";

    public void printDoctor(Project project,
                            ServerRuntimeExtension runtimeExt,
                            ClientInstallationExtension installExt) {

        File clientInstallDirectory = new File(installExt.getClientInstallDirectory().get());
        File clientServerJarFile = installExt.provideClientFile(ClientFiles.SERVER_JAR).get().getAsFile();
        File clientAssetsFile = installExt.provideClientFile(ClientFiles.ASSETS_ZIP).get().getAsFile();
        File clientAOTFile = installExt.provideClientFile(ClientFiles.AOT_FILE).get().getAsFile();
        File clientSourcesFile = installExt.provideClientFile(ClientFiles.SOURCES_JAR).get().getAsFile();

        String clientServerVersion = JarManifestUtils.getVersion(clientServerJarFile);

        String runtimeDirectoryPath = runtimeExt.getRuntimeDirectory().getOrNull();
        File runtimeDirectory = null;
        String runtimeText = "Not configured";
        if (runtimeDirectoryPath != null && !runtimeDirectoryPath.trim().isEmpty()) {
            runtimeDirectory = runtimeExt.provideRuntimeDirectory(project);
            if(runtimeDirectory.exists() && !runtimeDirectory.isDirectory()) {
                throw new InvalidUserDataException("Configured runtimeDirectory is not a directory!");
            }
            if(!runtimeDirectory.exists()) {
                runtimeDirectory.mkdirs();
            }
            runtimeText = runtimeDirectory.getAbsolutePath();
        }

        boolean isServerRunnable = false;
        File serverJarFile = null;
        if (runtimeDirectory != null && runtimeDirectory.exists()) {
            serverJarFile = clientServerJarFile;
            if (serverJarFile.exists() && clientAssetsFile.exists()) {
                isServerRunnable = true;
            }
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
        PluginLog.print(manifestJson);
        PluginLog.print("============== JAR file ==============");
        PluginLog.print("");
        PluginLog.print(" JAR file build task > \"" + archiveTaskName + "\"");
        PluginLog.print(" JAR file build name > " + archiveFile.getName());
        PluginLog.print(" JAR file build path > " + archiveFile.getAbsolutePath());
        PluginLog.print("");
        PluginLog.print("=============== Runtime ==============");
        PluginLog.print("");
        PluginLog.print("  Server-Runtime-Directory > " + runtimeText);
        PluginLog.print("    Runtime-Server-Version > " + runtimeServerVersion);
        PluginLog.print("       Is server runnable? > " + booleanToHuman(isServerRunnable));
        PluginLog.print("   Version matches client? > " + matchesVersion);
        PluginLog.print("");
        PluginLog.print("======================================");
    }

    private String booleanToHuman(boolean value) {
        return value ? "YES" : "NO";
    }

    private String fileExists(File file) {
        if (file == null) {
            return NOT_FOUND;
        }
        return file.exists() ? FOUND : NOT_FOUND;
    }
}
