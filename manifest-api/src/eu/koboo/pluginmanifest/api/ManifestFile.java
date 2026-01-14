package eu.koboo.pluginmanifest.api;

import eu.koboo.pluginmanifest.api.validation.InvalidPluginManifestException;
import eu.koboo.pluginmanifest.api.validation.ManifestValidation;
import eu.koboo.pluginmanifest.api.validation.ValidationResult;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.util.*;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ManifestFile {

    public static final String NAME = "manifest.json";

    String pluginGroup;
    String pluginName;
    String pluginVersion;
    String pluginDescription;

    final List<ManifestAuthor> pluginAuthors = new ArrayList<>();

    String pluginWebsite;
    String pluginMainClass;

    // NOT SUPPORTED, needs SemVerRange implementation
    final String serverVersion = "*";

    final Map<String, String> dependencies = new LinkedHashMap<>();
    final Map<String, String> optionalDependencies = new LinkedHashMap<>();
    final Map<String, String> loadBefore = new LinkedHashMap<>();

    // NOT SUPPORTED, needs complex serilization
    final List<ManifestFile> subPlugins = new ArrayList<>();

    boolean disabledByDefault = false;
    boolean includesAssetPack = false;

    public void setPluginGroup(String pluginGroup) {
        this.pluginGroup = pluginGroup;
    }

    public void setPluginName(String pluginName) {
        this.pluginName = pluginName;
    }

    public void setPluginVersion(String pluginVersion) {
        this.pluginVersion = pluginVersion;
    }

    public void setPluginDescription(String description) {
        this.pluginDescription = description;
    }

    public void addPluginAuthor(String name, String email, String website) {
        pluginAuthors.add(new ManifestAuthor(name, email, website));
    }

    public void addPluginAuthor(String name, String email) {
        addPluginAuthor(name, email, null);
    }

    public void addPluginAuthor(String name) {
        addPluginAuthor(name, null, null);
    }

    public boolean hasAuthors() {
        return !pluginAuthors.isEmpty();
    }

    public void setPluginWebsite(String website) {
        this.pluginWebsite = website;
    }

    public void setPluginMainClass(String mainClass) {
        this.pluginMainClass = mainClass;
    }

    public void addRequiredDependency(String pluginIdentifier) {
        addRequiredDependency(pluginIdentifier, "*");
    }

    public void addRequiredDependency(String pluginIdentifier, String versionRange) {
        dependencies.put(pluginIdentifier, versionRange);
    }

    public void addOptionalDependency(String pluginIdentifier) {
        addOptionalDependency(pluginIdentifier, "*");
    }

    public void addOptionalDependency(String pluginIdentifier, String versionRange) {
        optionalDependencies.put(pluginIdentifier, versionRange);
    }

    public void addLoadBeforeDependency(String pluginIdentifier) {
        addLoadBeforeDependency(pluginIdentifier, "*");
    }

    public void addLoadBeforeDependency(String pluginIdentifier, String versionRange) {
        loadBefore.put(pluginIdentifier, versionRange);
    }

    public void setPluginDisabledByDefault(boolean disabledByDefault) {
        this.disabledByDefault = disabledByDefault;
    }

    public void setPluginIncludesAssetPack(boolean includesAssetPack) {
        this.includesAssetPack = includesAssetPack;
    }

    private void validateManifest() throws InvalidPluginManifestException {

        List<ValidationResult> resultList = new ArrayList<>();

        ManifestValidation.validateCharacters(resultList, pluginGroup, "pluginGroup", '-');
        ManifestValidation.validateCharacters(resultList, pluginName, "pluginName", '-');
        ManifestValidation.validateSemVer(resultList, pluginVersion);
        ManifestValidation.validateWebsite(resultList, pluginWebsite);
        ManifestValidation.validateAuthors(resultList, pluginAuthors);
        ManifestValidation.validateRequired(resultList, pluginMainClass, "mainClass");
        ManifestValidation.validateDependencies(resultList, dependencies);
        ManifestValidation.validateDependencies(resultList, optionalDependencies);
        ManifestValidation.validateDependencies(resultList, loadBefore);
        ManifestValidation.validateMainClass(resultList, pluginMainClass);

        if (!resultList.isEmpty()) {
            throw new InvalidPluginManifestException(resultList);
        }
    }

    public Map<String, Object> asMap() throws InvalidPluginManifestException {

        validateManifest();

        Map<String, Object> manifestMap = new LinkedHashMap<>();

        manifestMap.put("Group", pluginGroup);
        manifestMap.put("Name", pluginName);
        manifestMap.put("Version", pluginVersion);
        if (pluginDescription != null && !pluginDescription.isEmpty()) {
            manifestMap.put("Description", pluginDescription);
        }

        List<Map<String, String>> authorList = new LinkedList<>();
        for (ManifestAuthor author : pluginAuthors) {
            Map<String, String> authorObject = new LinkedHashMap<>();
            authorObject.put("Name", author.getName());
            String email = author.getEmail();
            if (email != null) {
                authorObject.put("Email", email);
            }
            String url = author.getUrl();
            if (url != null) {
                authorObject.put("Url", url);
            }
            authorList.add(authorObject);
        }
        manifestMap.put("Authors", authorList);

        if (pluginWebsite != null) {
            manifestMap.put("Website", pluginWebsite);
        }

        manifestMap.put("ServerVersion", serverVersion);

        if (!dependencies.isEmpty()) {
            manifestMap.put("Dependencies", dependencies);
        }
        if (!optionalDependencies.isEmpty()) {
            manifestMap.put("OptionalDependencies", optionalDependencies);
        }
        if (!loadBefore.isEmpty()) {
            manifestMap.put("LoadBefore", loadBefore);
        }

        if (disabledByDefault) {
            manifestMap.put("DisabledByDefault", true);
        }
        if (includesAssetPack) {
            manifestMap.put("IncludesAssetPack", true);
        }

        manifestMap.put("Main", pluginMainClass);

        return manifestMap;
    }
}
