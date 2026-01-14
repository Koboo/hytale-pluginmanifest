package eu.koboo.pluginmanifest.gradle;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import eu.koboo.pluginmanifest.api.validation.InvalidPluginManifestException;
import eu.koboo.pluginmanifest.api.ManifestFile;
import eu.koboo.pluginmanifest.gradle.configs.ManifestConfig;
import groovy.json.JsonOutput;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.gradle.api.GradleException;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class PluginManifestPlugin implements Plugin<Project> {

    public static final String EXTENSION_NAME = "pluginManifest";
    public static final String TASK_NAME = "generateManifestJson";

    @Override
    public void apply(Project target) {
        ManifestConfig manifestConfig = target.getExtensions()
                .create(EXTENSION_NAME, ManifestConfig.class);

        File resourceDirectory = getGeneratedResourceDirectory(target);

        TaskProvider<PluginManifestTask> provider = target.getTasks().register(TASK_NAME, PluginManifestTask.class);

        target.afterEvaluate(project -> {
            SourceSet mainSourceSet = getMainSources(project);
            mainSourceSet.getResources().srcDir(resourceDirectory);

            applyDefaultValues(project, manifestConfig);
            String manifestJson = buildJson(manifestConfig);

            provider.configure(task -> {
                task.plugin = this;
                task.setGroup("other");
                task.getOutputFile().set(new File(resourceDirectory, ManifestFile.NAME));
                task.getManifestJson().set(manifestJson);
            });

            project.getTasks().named(mainSourceSet.getProcessResourcesTaskName(), task -> task.dependsOn(provider));
        });
    }

    private File getGeneratedResourceDirectory(Project project) {
        DirectoryProperty directoryProperty = project.getLayout().getBuildDirectory();
        File buildDirectory = directoryProperty.get().getAsFile();
        return new File(buildDirectory, "generated/pluginmanifest/");
    }

    private SourceSet getMainSources(Project project) {
        return project.getExtensions()
                .getByType(SourceSetContainer.class)
                .getByName("main");
    }

    private void applyDefaultValues(Project project, ManifestConfig config) {
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
            log(project, "Automatically applied project groupId as pluginGroup \"" + defaultPluginGroup + "\"!");
        }

        String pluginName = manifestFile.getPluginName();
        if (pluginName == null || pluginName.isEmpty()) {
            String projectArtifactId = project.getName();
            manifestFile.setPluginName(projectArtifactId);
            usesDefaultValue = true;
            log(project, "Automatically applied project name as pluginName \"" + projectArtifactId + "\"!");
        }

        String pluginVersion = manifestFile.getPluginVersion();
        if (pluginVersion == null || pluginVersion.isEmpty()) {
            String projectVersion = project.getVersion().toString();
            manifestFile.setPluginVersion(projectVersion);
            usesDefaultValue = true;
            log(project, "Automatically applied project version as pluginVersion \"" + projectVersion + "\"!");
        }

        if (!manifestFile.hasAuthors()) {
            String userName = System.getProperty("user.name");
            manifestFile.addPluginAuthor(userName);
            usesDefaultValue = true;
            log(project, "Automatically applied current user as pluginAuthor \"" + userName + "\"!");
        }

        String pluginMainClass = manifestFile.getPluginMainClass();
        if (pluginMainClass == null || pluginMainClass.isEmpty()) {
            List<String> candidates = getMainClassCandidates(project);
            if (candidates.isEmpty()) {
                log(project, "Couldn't find any pluginMainClass! Please configure the correct one manually.");
            }
            int candidateAmount = candidates.size();
            if (candidateAmount > 1) {
                log(project, "Found " + candidateAmount + " pluginMainClasses! Please configure the correct one manually.");
            }
            if (candidateAmount == 1) {
                String mainClassCandidate = candidates.getFirst();
                manifestFile.setPluginMainClass(mainClassCandidate);
                usesDefaultValue = true;
                log(project, "Found pluginMainClass: \"" + mainClassCandidate + "\"");
            }
        }

        if (usesDefaultValue) {
            log(project, "pluginmanifest automatically applied values!");
            log(project, "You can manually override values in pluginManifest{}");
        }
    }

    private String buildJson(ManifestConfig extension) {
        ManifestFile manifestFile = extension.getManifestFile();

        Map<String, Object> manifestMap;
        try {
            manifestMap = manifestFile.asMap();
        } catch (InvalidPluginManifestException e) {
            throw new InvalidUserDataException("PluginManifest Error: " + "PluginManifest Error: " + System.lineSeparator() + e.buildMessage());
        }

        String json = JsonOutput.toJson(manifestMap);

        if (!extension.isMinimizeJson()) {
            json = JsonOutput.prettyPrint(json);
        }
        return json;
    }

    private List<String> getMainClassCandidates(Project project) {
        SourceSet mainSourceSet = getMainSources(project);
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

    private void log(Project project, String message) {
        project.getLogger().lifecycle("> PluginManifest :applyDefaults:" + message);
    }
}
