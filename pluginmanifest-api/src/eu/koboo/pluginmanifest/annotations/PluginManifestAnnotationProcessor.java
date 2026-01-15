package eu.koboo.pluginmanifest.annotations;

import eu.koboo.pluginmanifest.annotations.api.PluginAuthor;
import eu.koboo.pluginmanifest.annotations.api.PluginDependency;
import eu.koboo.pluginmanifest.annotations.api.PluginManifest;
import eu.koboo.pluginmanifest.manifest.ManifestFile;
import eu.koboo.pluginmanifest.manifest.validation.InvalidPluginManifestException;
import org.json.JSONObject;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.Set;

@SupportedAnnotationTypes("eu.koboo.pluginmanifest.annotations.api.*")
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public class PluginManifestAnnotationProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment environment) {
        Set<? extends Element> possibleMainClassSet = environment.getElementsAnnotatedWith(PluginManifest.class);
        if (possibleMainClassSet.isEmpty()) {
            log("No class with annotation \"@PluginManifest\" found.");
            return false;
        }
        if (possibleMainClassSet.size() != 1) {
            log("More than one class with annotation \"@PluginManifest\" found!");
            return false;
        }

        Element mainElement = possibleMainClassSet.iterator().next();
        TypeElement pluginElement = ProcessorUtils.getTypeElement(processingEnv, mainElement);
        String fullyQualifiedName = pluginElement.getQualifiedName().toString();

        PluginManifest annotation = pluginElement.getAnnotation(PluginManifest.class);
        if (annotation == null) {
            log("Class is not annoted with \"@PluginManifest\".");
            return false;
        }

        ManifestFile manifestFile = new ManifestFile();
        manifestFile.setPluginGroup(annotation.group());
        manifestFile.setPluginName(annotation.name());
        manifestFile.setPluginVersion(annotation.version());
        manifestFile.setServerVersion(annotation.serverVersion());
        manifestFile.setPluginDescription(annotation.description());

        PluginAuthor[] authors = annotation.authors();
        for (PluginAuthor author : authors) {
            manifestFile.addPluginAuthor(author.name(), author.email(), author.url());
        }

        manifestFile.setPluginWebsite(annotation.website());
        PluginDependency[] dependencies = annotation.dependencies();
        for (PluginDependency dependency : dependencies) {
            switch (dependency.type()) {
                case REQUIRED -> manifestFile.addRequiredDependency(dependency.pluginId(), dependency.version());
                case OPTIONAL -> manifestFile.addOptionalDependency(dependency.pluginId(), dependency.version());
                case LOAD_BEFORE -> manifestFile.addLoadBeforeDependency(dependency.pluginId(), dependency.version());
            }
        }

        manifestFile.setDisabledByDefault(annotation.disabledByDefault());
        manifestFile.setIncludesAssetPack(annotation.includesAssetPack());
        manifestFile.setPluginMainClass(fullyQualifiedName);

        FileObject resourceFile;
        try {
            resourceFile = processingEnv.getFiler().createResource(
                StandardLocation.CLASS_OUTPUT,
                "",
                "manifest.json"
            );
        } catch (IOException e) {
            if(e instanceof FilerException) {
                log("manifest.json already existing!");
            }
            return false;
        }

        Map<String, Object> manifestMap;
        try {
            manifestMap = manifestFile.asMap();
        } catch (InvalidPluginManifestException e) {
            log(System.lineSeparator() + e.buildMessage());
            return false;
        }

        JSONObject jsonObject = new JSONObject(manifestMap);
        int indentFactor = 2;
        String json = jsonObject.toString(indentFactor);

        try (Writer writer = resourceFile.openWriter()) {
            writer.write(json);
            writer.flush();
        } catch (IOException e) {
            log("Writing manifest.json produced error: " + e.getMessage());
            return false;
        }
        return true;
    }

    private void log(String message) {
        ProcessorUtils.error(processingEnv, "PluginManifest: " + message);
    }
}
