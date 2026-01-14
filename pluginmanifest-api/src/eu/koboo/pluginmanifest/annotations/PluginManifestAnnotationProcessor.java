package eu.koboo.pluginmanifest.annotations;

import eu.koboo.pluginmanifest.annotations.api.PluginAuthor;
import eu.koboo.pluginmanifest.annotations.api.PluginDependency;
import eu.koboo.pluginmanifest.annotations.api.PluginManifest;
import eu.koboo.pluginmanifest.manifest.ManifestFile;
import eu.koboo.pluginmanifest.manifest.validation.InvalidPluginManifestException;
import org.json.JSONObject;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Map;
import java.util.Set;

@SupportedAnnotationTypes("eu.koboo.pluginmanifest.annotations.*")
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public class PluginManifestAnnotationProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment environment) {
        Set<? extends Element> possibleMainClassSet = environment.getElementsAnnotatedWith(PluginManifest.class);
        if (possibleMainClassSet.isEmpty()) {
            return false;
        }
        if (possibleMainClassSet.size() != 1) {
            return false;
        }
        Element mainElement = possibleMainClassSet.iterator().next();
        TypeElement pluginElement = ProcessorUtils.getTypeElement(processingEnv, mainElement);
        String fullyQualifiedName = pluginElement.getQualifiedName().toString();

        PluginManifest annotation = pluginElement.getAnnotation(PluginManifest.class);
        if (annotation == null) {
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

        Map<String, Object> manifestMap;
        try {
            manifestMap = manifestFile.asMap();
        } catch (InvalidPluginManifestException e) {
            ProcessorUtils.error(processingEnv, "PluginManifest Error: " + System.lineSeparator() + e.buildMessage());
            return false;
        }
        JSONObject jsonObject = new JSONObject(manifestMap);
        int indentFactor = 2;
        String json = jsonObject.toString(indentFactor);
        ProcessorUtils.writeResourceFile(processingEnv, json, "manifest.json");
        return true;
    }
}
