package eu.koboo.pluginmanifest.gradle;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import eu.koboo.pluginmanifest.gradle.configs.PluginManifestExtension;
import eu.koboo.pluginmanifest.manifest.ManifestFile;
import eu.koboo.pluginmanifest.manifest.validation.InvalidPluginManifestException;
import groovy.json.JsonOutput;
import lombok.experimental.UtilityClass;
import org.gradle.api.GradleException;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@UtilityClass
public class ManifestUtils {

    public String buildJson(PluginManifestExtension extension) {
        ManifestFile manifestFile = extension.getManifestFile();

        Map<String, Object> manifestMap;
        try {
            manifestMap = manifestFile.asMap();
        } catch (InvalidPluginManifestException e) {
            throw new InvalidUserDataException("PluginManifest Error: " + System.lineSeparator() + e.buildMessage());
        }

        String json = JsonOutput.toJson(manifestMap);

        if (!extension.isMinimizeJson()) {
            json = JsonOutput.prettyPrint(json);
        }
        return json;
    }

    void applyAutomaticProperties(PluginManifestPlugin plugin, Project project, PluginManifestExtension config) {
        ManifestFile manifestFile = config.getManifestFile();

        boolean usesDefaultValue = false;

        String userPluginGroup = manifestFile.getPluginGroup();
        if (userPluginGroup == null || userPluginGroup.isEmpty()) {
            String projectGroupId = project.getGroup().toString();
            String defaultPluginGroup;
            if (projectGroupId.contains(".")) {
                String[] groupSegments = projectGroupId.split("\\.");
                defaultPluginGroup = groupSegments[groupSegments.length - 1];
            } else {
                defaultPluginGroup = projectGroupId;
            }
            manifestFile.setPluginGroup(defaultPluginGroup);
            usesDefaultValue = true;
            plugin.log(project, "applyProperties", "Automatically applied project groupId as pluginGroup \"" + defaultPluginGroup + "\"!");
        }

        String pluginName = manifestFile.getPluginName();
        if (pluginName == null || pluginName.isEmpty()) {
            String projectArtifactId = project.getName();
            manifestFile.setPluginName(projectArtifactId);
            usesDefaultValue = true;
            plugin.log(project, "applyProperties", "Automatically applied project name as pluginName \"" + projectArtifactId + "\"!");
        }

        String pluginVersion = manifestFile.getPluginVersion();
        if (pluginVersion == null || pluginVersion.isEmpty()) {
            String projectVersion = project.getVersion().toString();
            manifestFile.setPluginVersion(projectVersion);
            usesDefaultValue = true;
            plugin.log(project, "applyProperties", "Automatically applied project version as pluginVersion \"" + projectVersion + "\"!");
        }

        if (manifestFile.getPluginAuthors().isEmpty()) {
            String userName = System.getProperty("user.name");
            manifestFile.addPluginAuthor(userName, null, null);
            usesDefaultValue = true;
            plugin.log(project, "applyProperties", "Automatically applied current user as pluginAuthor \"" + userName + "\"!");
        }

        String pluginMainClass = manifestFile.getPluginMainClass();
        if (pluginMainClass == null || pluginMainClass.isEmpty()) {
            List<String> candidates = getMainClassCandidates(project);
            if (candidates.isEmpty()) {
                plugin.log(project, "applyProperties", "Couldn't find any pluginMainClass! Please configure the correct one manually.");
            }
            int candidateAmount = candidates.size();
            if (candidateAmount > 1) {
                plugin.log(project, "applyProperties", "Found " + candidateAmount + " pluginMainClasses! Please configure the correct one manually.");
            }
            if (candidateAmount == 1) {
                String mainClassCandidate = candidates.getFirst();
                manifestFile.setPluginMainClass(mainClassCandidate);
                usesDefaultValue = true;
                plugin.log(project, "applyProperties", "Found pluginMainClass: \"" + mainClassCandidate + "\"");
            }
        }

        if (usesDefaultValue) {
            plugin.log(project, "applyProperties", "pluginmanifest automatically applied values!");
            plugin.log(project, "applyProperties", "You can manually override values in pluginManifest{}");
        }
    }

    private List<String> getMainClassCandidates(Project project) {
        SourceSet mainSourceSet = GradleUtils.getMainSourceSet(project);
        Set<File> javaSrcDirs = mainSourceSet.getJava().getSrcDirs();
        List<String> candidates = new ArrayList<>();

        JavaParser parser = new JavaParser();

        for (File srcDir : javaSrcDirs) {
            if (!srcDir.exists()) {
                continue;
            }
            List<Path> javaFiles;
            try {
                javaFiles = Files.walk(srcDir.toPath())
                    .filter(p -> p.toString().endsWith(".java"))
                    .toList();
            } catch (IOException e) {
                throw new GradleException("Couldn't detect mainClass: ", e);
            }
            if (javaFiles.isEmpty()) {
                continue;
            }
            for (Path filePath : javaFiles) {
                CompilationUnit compilationUnit;
                try {
                    Optional<CompilationUnit> result = parser.parse(filePath)
                        .getResult();
                    if (result.isEmpty()) {
                        continue;
                    }
                    compilationUnit = result.get();
                } catch (IOException e) {
                    // Silent ignore.
                    continue;
                }
                List<ClassOrInterfaceDeclaration> classDeclarationList = compilationUnit
                    .findAll(ClassOrInterfaceDeclaration.class)
                    .stream()
                    .filter(c -> !c.isInterface())
                    .filter(c -> c.getExtendedTypes().stream().anyMatch(t -> t.getNameAsString().equals("JavaPlugin")))
                    .filter(c -> !c.isNestedType())
                    .toList();
                for (ClassOrInterfaceDeclaration classDeclaration : classDeclarationList) {
                    String classPackage = compilationUnit.getPackageDeclaration()
                        .map(NodeWithName::getNameAsString)
                        .orElse("");
                    String fullyQualifiedClassName = classPackage.isEmpty()
                        ? classDeclaration.getNameAsString()
                        : classPackage + "." + classDeclaration.getNameAsString();
                    candidates.add(fullyQualifiedClassName);
                }
            }
        }
        return candidates;
    }
}
