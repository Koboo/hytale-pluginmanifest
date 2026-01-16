package eu.koboo.pluginmanifest.gradle.plugin.extension.manifest;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.tasks.Input;

import javax.inject.Inject;
import java.util.LinkedHashMap;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public abstract class ManifestPluginDependencyExtension {

    @Input
    MapProperty<String, String> requiredDependencies;
    @Input
    MapProperty<String, String> optionalDependencies;
    @Input
    MapProperty<String, String> loadBeforeDependencies;

    @Inject
    public ManifestPluginDependencyExtension(ObjectFactory objectFactory) {
        requiredDependencies = objectFactory.mapProperty(String.class, String.class);
        optionalDependencies = objectFactory.mapProperty(String.class, String.class);
        loadBeforeDependencies = objectFactory.mapProperty(String.class, String.class);

        requiredDependencies.convention(new LinkedHashMap<>());
        optionalDependencies.convention(new LinkedHashMap<>());
        loadBeforeDependencies.convention(new LinkedHashMap<>());
    }

    public void required(String pluginIdentifier) {
        required(pluginIdentifier, "*");
    }

    public void required(String pluginIdentifier, String version) {
        requiredDependencies.put(pluginIdentifier, version);
    }

    public void optional(String pluginIdentifier) {
        optional(pluginIdentifier, "*");
    }

    public void optional(String pluginIdentifier, String version) {
        optionalDependencies.put(pluginIdentifier, version);
    }

    public void loadBefore(String pluginIdentifier) {
        loadBefore(pluginIdentifier, "*");
    }

    public void loadBefore(String pluginIdentifier, String version) {
        loadBeforeDependencies.put(pluginIdentifier, version);
    }
}
