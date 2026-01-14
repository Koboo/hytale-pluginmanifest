package eu.koboo.pluginmanifest.gradle.configs;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter(AccessLevel.PROTECTED)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ManifestDependencyConfig {

    Map<String, String> requiredDependencies = new LinkedHashMap<>();
    Map<String, String> optionalDependencies = new LinkedHashMap<>();
    Map<String, String> loadBeforeDependencies = new LinkedHashMap<>();

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
