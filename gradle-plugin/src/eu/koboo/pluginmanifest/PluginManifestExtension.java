package eu.koboo.pluginmanifest;

import eu.koboo.pluginmanifest.model.AuthorInfo;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.gradle.api.InvalidUserDataException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter(AccessLevel.PACKAGE)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PluginManifestExtension {

    // Not null
    String pluginGroup;
    // Not null
    String pluginName;
    // Not null, semver format
    String pluginVersion;
    // Nullable
    String pluginDescription;

    final List<AuthorInfo> authors = new ArrayList<>();

    // Nullable
    String pluginWebsite;

    // Can be nullable, but why should it be?
    String pluginMain;

    // NOT SUPPORTED
    final String serverVersion = "*";

    // SEMVER RANGE NOT VALIDATED
    final Map<String, String> dependencies = new LinkedHashMap<>();
    final Map<String, String> optionalDependencies = new LinkedHashMap<>();
    final Map<String, String> loadBefore = new LinkedHashMap<>();

    // NOT SUPPORTED
    final List<String> subPlugins = new ArrayList<>();

    boolean disabledByDefault = false;
    boolean includesAssetPack = false;

    public void setPluginGroup(String pluginGroup) {
        if (pluginGroup == null || pluginGroup.isEmpty()) {
            throw new InvalidUserDataException("pluginGroup cannot be null or empty");
        }
        this.pluginGroup = pluginGroup;
    }

    public void setPluginName(String pluginName) {
        if (pluginName == null || pluginName.isEmpty()) {
            throw new InvalidUserDataException("pluginName cannot be null or empty");
        }
        this.pluginName = pluginName;
    }

    public void setPluginVersion(String pluginVersion) {
        if (pluginVersion == null || pluginVersion.isEmpty()) {
            throw new InvalidUserDataException("pluginVersion cannot be null or empty");
        }
        try {
            String[] split = pluginVersion.split("\\.");
            if (split.length != 3) {
                throw new IllegalArgumentException();
            }
            // Major
            Integer.parseInt(split[0]);
            // Minor
            Integer.parseInt(split[1]);
            // Patch
            Integer.parseInt(split[2]);
        } catch (Exception e) {
            throw new InvalidUserDataException("pluginVersion is not in semver format. (major.minor.patch)");
        }
        this.pluginVersion = pluginVersion;
    }

    public void setPluginDescription(String description) {
        this.pluginDescription = description;
    }

    public void addPluginAuthor(String name, String email, String website) {
        authors.add(new AuthorInfo(name, email, website));
    }

    public void addPluginAuthor(String name, String email) {
        addPluginAuthor(name, email, null);
    }

    public void addPluginAuthor(String name) {
        addPluginAuthor(name, null, null);
    }

    public void setPluginWebsite(String website) {
        this.pluginWebsite = website;
    }

    public void setPluginMainClass(String mainClass) {
        if(mainClass == null || mainClass.isEmpty()) {
            throw new InvalidUserDataException("mainClass cannot be null or empty");
        }
        this.pluginMain = mainClass;
    }

    public void addPluginDependency(String pluginIdentifier) {
        if(pluginIdentifier != null && !pluginIdentifier.contains(":")) {
            throw new InvalidUserDataException("pluginIdentifier format invalid! (Group:Name)");
        }
        dependencies.put(pluginIdentifier, "*");
    }

    public void addOptionalPluginDependency(String pluginIdentifier) {
        if(pluginIdentifier != null && !pluginIdentifier.contains(":")) {
            throw new InvalidUserDataException("pluginIdentifier format invalid! (Group:Name)");
        }
        optionalDependencies.put(pluginIdentifier, "*");
    }

    public void addPluginLoadBefore(String pluginIdentifier) {
        if(pluginIdentifier != null && !pluginIdentifier.contains(":")) {
            throw new InvalidUserDataException("pluginIdentifier format invalid! (Group:Name)");
        }
        loadBefore.put(pluginIdentifier, "*");
    }

    public void setPluginDisabledByDefault(boolean disabledByDefault) {
        this.disabledByDefault = disabledByDefault;
    }

    public void setPluginIncludesAssetPack(boolean includesAssetPack) {
        this.includesAssetPack = includesAssetPack;
    }
}
