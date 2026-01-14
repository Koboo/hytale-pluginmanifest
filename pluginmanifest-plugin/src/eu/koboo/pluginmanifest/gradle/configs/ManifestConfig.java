package eu.koboo.pluginmanifest.gradle.configs;

import eu.koboo.pluginmanifest.manifest.ManifestFile;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.gradle.api.Action;

import java.util.List;
import java.util.Map;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class ManifestConfig {

    @Getter
    final ManifestFile manifestFile = new ManifestFile();
    final ManifestAuthorsConfig authorsConfig = new ManifestAuthorsConfig();
    final ManifestDependencyConfig dependencyConfig = new ManifestDependencyConfig();

    @Getter
    boolean minimizeJson = false;

    public void pluginMeta(String pluginGroup, String pluginName, String pluginVersion) {
        manifestFile.setPluginGroup(pluginGroup);
        manifestFile.setPluginName(pluginName);
        manifestFile.setPluginVersion(pluginVersion);
    }

    public void pluginGroup(String pluginGroup) {
        manifestFile.setPluginGroup(pluginGroup);
    }

    public String pluginGroup() {
        return manifestFile.getPluginGroup();
    }

    public void pluginName(String pluginName) {
        manifestFile.setPluginName(pluginName);
    }

    public String pluginName() {
        return manifestFile.getPluginName();
    }

    public void pluginVersion(String pluginVersion) {
        manifestFile.setPluginVersion(pluginVersion);
    }

    public String pluginVersion() {
        return manifestFile.getPluginVersion();
    }

    public void pluginDescription(String description) {
        manifestFile.setPluginDescription(description);
    }

    public String pluginDescription() {
        return manifestFile.getPluginDescription();
    }

    public void pluginWebsite(String website) {
        manifestFile.setPluginWebsite(website);
    }

    public String pluginWebsite() {
        return manifestFile.getPluginWebsite();
    }

    public void pluginMainClass(String mainClass) {
        manifestFile.setPluginMainClass(mainClass);
    }

    public String pluginMainClass() {
        return manifestFile.getPluginMainClass();
    }

    public void pluginDisabledByDefault(boolean disabledByDefault) {
        manifestFile.setDisabledByDefault(disabledByDefault);
    }

    public boolean pluginIsDisabledByDefault() {
        return manifestFile.isDisabledByDefault();
    }

    public void pluginIncludesAssetPack(boolean includesAssetPack) {
        manifestFile.setIncludesAssetPack(includesAssetPack);
    }

    public boolean pluginIncludesAssetPack() {
        return manifestFile.isIncludesAssetPack();
    }

    public void minimizeJson(boolean minimizeJson) {
        this.minimizeJson = minimizeJson;
    }

    public boolean minimizeJson() {
        return minimizeJson;
    }

    public void authors(Action<ManifestAuthorsConfig> action) {
        action.execute(authorsConfig);
        manifestFile.getPluginAuthors().clear();
        List<ManifestAuthorConfig> authorConfigList = authorsConfig.getAuthorConfigList();
        for (ManifestAuthorConfig manifestAuthorConfig : authorConfigList) {
            manifestFile.addPluginAuthor(
                manifestAuthorConfig.authorName(),
                manifestAuthorConfig.authorEmail(),
                manifestAuthorConfig.authorUrl()
            );
        }
    }

    public void pluginDependencies(Action<ManifestDependencyConfig> action) {
        action.execute(dependencyConfig);
        updateFileDependencies(manifestFile.getDependencies(), dependencyConfig.getRequiredDependencies());
        updateFileDependencies(manifestFile.getOptionalDependencies(), dependencyConfig.getOptionalDependencies());
        updateFileDependencies(manifestFile.getLoadBefore(), dependencyConfig.getLoadBeforeDependencies());
    }

    private void updateFileDependencies(Map<String, String> fileDependencies, Map<String, String> configDependencies) {
        fileDependencies.clear();
        for (String pluginIdentifier : configDependencies.keySet()) {
            String pluginVersion = configDependencies.get(pluginIdentifier);
            fileDependencies.put(pluginIdentifier, pluginVersion);
        }
    }
}
