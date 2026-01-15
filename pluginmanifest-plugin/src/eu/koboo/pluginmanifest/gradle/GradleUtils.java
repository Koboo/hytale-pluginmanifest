package eu.koboo.pluginmanifest.gradle;

import lombok.experimental.UtilityClass;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

import java.io.File;

@UtilityClass
public class GradleUtils {

    public File getDirectoryInBuild(Project project, String newDirectory) {
        DirectoryProperty directoryProperty = project.getLayout().getBuildDirectory();
        File buildDirectory = directoryProperty.get().getAsFile();
        if (newDirectory.startsWith("/")) {
            newDirectory = newDirectory.substring(1);
        }
        if (!newDirectory.endsWith("/")) {
            newDirectory = newDirectory + "/";
        }
        return new File(buildDirectory, newDirectory);
    }

    public SourceSet getMainSourceSet(Project project) {
        return project.getExtensions()
            .getByType(SourceSetContainer.class)
            .getByName("main");
    }

    public File getRootProjectDirectory(Project project) {
        Project rootProject = project.getRootProject();
        return rootProject.getLayout().getProjectDirectory().getAsFile();
    }

    public String getAppDataDirectory() {
        String osName = System.getProperty("os.name");
        if(osName == null || osName.trim().isEmpty()) {
            return null;
        }
        if(osName.startsWith("Windows")) {
            return System.getenv("APPDATA");
        }
        if(osName.startsWith("Mac")) {
            String userHome = System.getProperty("user.home");
            if(userHome == null || userHome.trim().isEmpty()) {
                return null;
            }
            return userHome + "/Library/Application Support/";
        }
        if(osName.startsWith("Linux")) {
            return System.getenv("XDG_DATA_HOME");
        }
        return null;
    }
}
