package eu.koboo.pluginmanifest.gradle;

import eu.koboo.pluginmanifest.gradle.configs.PluginManifestExtension;
import lombok.experimental.UtilityClass;
import org.gradle.api.Project;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class DependencyUtils {

    public File searchServerFile(PluginManifestPlugin plugin, Project project, PluginManifestExtension extension) {
        String rootDirectory = GradleUtils.getRootProjectDirectory(project).getAbsolutePath();
        if(!rootDirectory.endsWith("/")) {
            rootDirectory += "/";
        }

        List<String> searchedPathList = new ArrayList<>();

        String serverJarPath = extension.serverJarPath();
        if(serverJarPath != null) {
            if(serverJarPath.trim().isEmpty()) {
                plugin.log(project, "searchServerFile", "Provided serverJarPath is empty!");
                return null;
            }
            if(!serverJarPath.endsWith(".jar")) {
                plugin.log(project, "searchServerFile", "Provided serverJarPath doesn't end with \".jar\"!");
                plugin.log(project, "searchServerFile", "serverJarPath=" + serverJarPath);
                return null;
            }
            File providedJarFile = new File(serverJarPath);
            if(!providedJarFile.exists()) {
                plugin.log(project, "searchServerFile", "Provided serverJarFile doesn't exist!");
                plugin.log(project, "searchServerFile", "serverJarPath=" + serverJarPath);
                return null;
            }
            return providedJarFile;
        }

        String serverJarFileName = PluginManifestPlugin.SERVER_JAR_NAME;
        // Try searching in "{PROJECT}/{JAR_NAME}"
        File serverJarFile = new File(rootDirectory + serverJarFileName);
        if(!serverJarFile.exists()) {
            searchedPathList.add(serverJarFile.getAbsolutePath());

            // Try searching in "{PROJECT}/libs/{JAR_NAME}"
            String libsDirectory = PluginManifestPlugin.LIBS_DIRECTORY;
            serverJarFile = new File(rootDirectory + libsDirectory + serverJarFileName);
        }

        if(!serverJarFile.exists()) {
            searchedPathList.add(serverJarFile.getAbsolutePath());

            String appDataDirectory = GradleUtils.getAppDataDirectory();
            if (!appDataDirectory.endsWith("/")) {
                appDataDirectory += "/";
            }
            String clientServerPath = PluginManifestPlugin.CLIENT_SERVER_PATH;

            // Try searching in "{APPDATA}/Hytale/install/release/package/game/latest/Server/{JAR_NAME}"
            serverJarFile = new File(appDataDirectory + clientServerPath + serverJarFileName);
        }

        if(!serverJarFile.exists()) {
            searchedPathList.add(serverJarFile.getAbsolutePath());
            plugin.log(project, "searchServerFile", "Couldn't add hytale dependency to project!");
            plugin.log(project, "searchServerFile", "Couldn't find \"" + serverJarFileName + "\" in any location:");
            for (String searchedPath : searchedPathList) {
                plugin.log(project, "searchServerFile", "- \"" + searchedPath + "\"");
            }
            return null;
        }
        return serverJarFile;
    }
}
