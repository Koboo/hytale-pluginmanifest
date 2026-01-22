package eu.koboo.pluginmanifest.gradle.plugin.extension.manifest;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.gradle.api.Action;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

import javax.inject.Inject;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public abstract class JsonManifestExtension {

    Property<String> pluginGroup;
    Property<String> pluginName;
    Property<String> pluginVersion;
    Property<String> serverVersion;
    Property<String> pluginMainClass;

    Property<String> pluginDescription;
    Property<String> pluginWebsite;

    Property<Boolean> disabledByDefault;
    Property<Boolean> includesAssetPack;
    Property<Boolean> minimizeJson;

    ManifestAuthorsExtension manifestAuthors;
    ManifestPluginDependencyExtension manifestPluginDependencies;

    @Inject
    public JsonManifestExtension(ObjectFactory objectFactory) {
        this.pluginGroup = objectFactory.property(String.class);
        this.pluginName = objectFactory.property(String.class);
        this.pluginVersion = objectFactory.property(String.class);
        this.serverVersion = objectFactory.property(String.class);
        this.pluginMainClass = objectFactory.property(String.class);

        this.pluginDescription = objectFactory.property(String.class);
        this.pluginWebsite = objectFactory.property(String.class);

        this.disabledByDefault = objectFactory.property(Boolean.class);
        this.includesAssetPack = objectFactory.property(Boolean.class);

        this.minimizeJson = objectFactory.property(Boolean.class);

        this.manifestAuthors = objectFactory.newInstance(ManifestAuthorsExtension.class);
        this.manifestPluginDependencies = objectFactory.newInstance(ManifestPluginDependencyExtension.class);
    }

    public void authors(Action<ManifestAuthorsExtension> action) {
        action.execute(manifestAuthors);
    }

    public void pluginDependencies(Action<ManifestPluginDependencyExtension> action) {
        action.execute(manifestPluginDependencies);
    }
}
