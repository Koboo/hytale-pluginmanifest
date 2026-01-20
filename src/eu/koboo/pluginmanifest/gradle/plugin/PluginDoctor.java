package eu.koboo.pluginmanifest.gradle.plugin;

import eu.koboo.pluginmanifest.gradle.plugin.extension.clientinstall.ClientInstallationExtension;
import eu.koboo.pluginmanifest.gradle.plugin.extension.serverruntime.ServerRuntimeExtension;
import eu.koboo.pluginmanifest.gradle.plugin.utils.JarManifestUtils;
import eu.koboo.pluginmanifest.gradle.plugin.utils.JavaSourceUtils;
import eu.koboo.pluginmanifest.gradle.plugin.utils.PluginLog;
import lombok.experimental.UtilityClass;
import org.gradle.api.Project;
import org.gradle.jvm.tasks.Jar;

import java.io.File;

@UtilityClass
public class PluginDoctor {

    private static final String FOUND = "[✓] Found";
    private static final String NOT_FOUND = "[ ] Not found";

    public void printDoctor(Project project,
                            ServerRuntimeExtension runtimeExt,
                            ClientInstallationExtension installExt,
                            boolean hasAnyResources,
                            String mainClass) {

        File clientInstallDirectory = installExt.resolveClientInstallDirectory();
        File clientServerJarFile = installExt.resolveClientServerJarFile();

        String clientServerVersion = JarManifestUtils.getVersion(clientServerJarFile);

        String infoRuntimeDirectory = "Not configured";
        String runtimeDirectoryPath = runtimeExt.getRuntimeDirectory().getOrNull();
        File runtimeDirectory = null;
        if (runtimeDirectoryPath != null && !runtimeDirectoryPath.trim().isEmpty()) {
            runtimeDirectory = new File(runtimeDirectoryPath);
            infoRuntimeDirectory = runtimeDirectory.getAbsolutePath();
        }

        boolean isServerRunnable = false;
        File runtimeServerJarFile = null;
        if (runtimeDirectory != null && runtimeDirectory.exists()) {
            runtimeServerJarFile = runtimeExt.resolveRuntimeServerJarFile();
            File runtimeAOTFile = runtimeExt.resolveRuntimeAOTFile();
            File runtimeAssetsFile = runtimeExt.resolveRuntimeAssetsFile();
            if (runtimeServerJarFile.exists() && runtimeAOTFile.exists() && runtimeAssetsFile.exists()) {
                isServerRunnable = true;
            }
        }

        // Parse versions by MANIFEST of client and runtime server jar
        String runtimeServerVersion = JarManifestUtils.getVersion(runtimeServerJarFile);
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

        PluginLog.info("PluginManifest doctor results for \"" + project.getName() + "\":");
        PluginLog.print("========= Client Installation ========");
        PluginLog.print("");
        PluginLog.print("                      Path > " + clientInstallDirectory.getAbsolutePath());
        PluginLog.print("                 Patchline > " + installExt.resolvePatchlineName());
        PluginLog.print("            Server-Version > " + JarManifestUtils.getVersion(clientServerJarFile));
        PluginLog.print("        'HytaleServer.jar' > " + fileExists(clientServerJarFile));
        PluginLog.print("        'HytaleServer.aot' > " + fileExists(installExt.resolveClientAOTFile()));
        PluginLog.print("'HytaleServer-sources.jar' > " + fileExists(installExt.resolveClientServerSourcesFile()));
        PluginLog.print("              'Assets.zip' > " + fileExists(installExt.resolveClientAssetsFile()));
        PluginLog.print("");
        PluginLog.print("============== Manifest ==============");
        PluginLog.print("");
        PluginLog.print(" 'IncludesAssetPack' > \"" + hasAnyResources + "\"");
        PluginLog.print("              'Main' > \"" + mainClass + "\"");
        PluginLog.print("");
        PluginLog.print("============== JAR file ==============");
        PluginLog.print("");
        PluginLog.print(" JAR file build task > \"" + archiveTaskName + "\"");
        PluginLog.print(" JAR file build name > " + archiveFile.getName());
        PluginLog.print(" JAR file build path > " + archiveFile.getAbsolutePath());
        PluginLog.print("");
        PluginLog.print("=============== Runtime ==============");
        PluginLog.print("");
        PluginLog.print("  Server-Runtime-Directory > " + infoRuntimeDirectory);
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
