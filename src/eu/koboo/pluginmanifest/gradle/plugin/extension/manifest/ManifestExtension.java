package eu.koboo.pluginmanifest.gradle.plugin.extension.manifest;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.gradle.api.Action;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;

import javax.inject.Inject;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public abstract class ManifestExtension {

    @Input
    @Optional
    Property<String> pluginGroup;
    @Input
    @Optional
    Property<String> pluginName;
    @Input
    @Optional
    Property<String> pluginVersion;
    @Input
    @Optional
    Property<String> serverVersion;
    @Input
    @Optional
    Property<String> pluginMainClass;

    @Input
    @Optional
    Property<String> pluginDescription;
    @Input
    @Optional
    Property<String> pluginWebsite;

    @Input
    @Optional
    Property<Boolean> disabledByDefault;
    @Input
    @Optional
    Property<Boolean> includesAssetPack;
    @Input
    @Optional
    Property<Boolean> minimizeJson;

    @Nested
    ManifestAuthorsExtension authors;
    @Nested
    ManifestPluginDependencyExtension pluginDependencies;

    @Inject
    public ManifestExtension(ObjectFactory objectFactory) {
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

        this.authors = objectFactory.newInstance(ManifestAuthorsExtension.class);
        this.pluginDependencies = objectFactory.newInstance(ManifestPluginDependencyExtension.class);
    }

    public void authors(Action<ManifestAuthorsExtension> action) {
        action.execute(authors);
    }

    public void pluginDependencies(Action<ManifestPluginDependencyExtension> action) {
        action.execute(pluginDependencies);
    }
}
