package eu.koboo.pluginmanifest.gradle.plugin.utils;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import lombok.experimental.UtilityClass;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.jvm.tasks.Jar;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@UtilityClass
public class JavaSourceUtils {
    private static final String DEFAULT = "jar";
    private static final String SHADOW = "shadowJar";

    public boolean hasAnyResource(@NotNull SourceSet sourceSet) {
        for (File srcDir : sourceSet.getResources().getSrcDirs()) {
            if(srcDir.exists() && containsFile(srcDir)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsFile(File dir) {
        File[] files = dir.listFiles();
        if (files == null) {
            return false;
        }
        for (File subFile : files) {
            if(subFile.isHidden()) {
                continue;
            }
            if (subFile.isFile()) {
                return true;
            }
            if (subFile.isDirectory() && containsFile(subFile)) {
                return true;
            }
        }
        return false;
    }

    public List<String> getMainClassCandidates(SourceSet mainSourceSet) {
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
                throw new GradleException("Can't detect mainClass: ", e);
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

    public @NotNull File resolveArchive(@NotNull Project project) {
        Jar shadowJarTask = project.getTasks()
            .withType(Jar.class)
            .stream()
            .filter(t -> t.getName().equals(SHADOW))
            .findFirst()
            .orElse(null);
        if (shadowJarTask != null) {
            return shadowJarTask.getArchiveFile()
                .get()
                .getAsFile();
        }
        return project.getTasks()
            .named(DEFAULT, Jar.class)
            .flatMap(Jar::getArchiveFile)
            .get()
            .getAsFile();
    }

    public @NotNull String resolveArchiveTaskName(@NotNull Project project) {
        Jar shadowJarTask = project.getTasks()
            .withType(Jar.class)
            .stream()
            .filter(t -> t.getName().equals(SHADOW))
            .findFirst()
            .orElse(null);
        if (shadowJarTask != null) {
            return SHADOW;
        }
        return DEFAULT;
    }
}
