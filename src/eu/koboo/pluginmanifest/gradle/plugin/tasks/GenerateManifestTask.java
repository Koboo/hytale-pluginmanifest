package eu.koboo.pluginmanifest.gradle.plugin.tasks;

import eu.koboo.pluginmanifest.gradle.plugin.extension.manifest.ManifestAuthorExtension;
import eu.koboo.pluginmanifest.gradle.plugin.extension.manifest.ManifestAuthorsExtension;
import eu.koboo.pluginmanifest.gradle.plugin.extension.manifest.ManifestExtension;
import eu.koboo.pluginmanifest.gradle.plugin.extension.manifest.ManifestPluginDependencyExtension;
import eu.koboo.pluginmanifest.gradle.plugin.tasks.validation.ManifestValidation;
import eu.koboo.pluginmanifest.gradle.plugin.tasks.validation.ValidationException;
import eu.koboo.pluginmanifest.gradle.plugin.utils.PluginLog;
import groovy.json.JsonOutput;
import lombok.extern.slf4j.Slf4j;
import org.gradle.api.DefaultTask;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Slf4j
public abstract class GenerateManifestTask extends DefaultTask {

    private static final String FALLBACK_PLUGIN_VERSION = "0.0.0";
    private static final String FALLBACK_SERVER_VERSION = "*";

    @Input
    public abstract Property<String> getResourceDirectory();

    @Input
    public abstract Property<String> getProjectGroupId();

    @Input
    public abstract Property<String> getProjectArtifactId();

    @Input
    public abstract Property<String> getProjectVersion();

    @Input
    public abstract ListProperty<String> getMainClassCandidates();

    @Input
    @Optional
    public abstract Property<String> getOSUserName();

    @Nested
    public abstract Property<ManifestExtension> getExtension();

    @TaskAction
    public void runTask() throws IOException {

        ManifestExtension extension = getExtension().get();

        Map<String, Object> manifestMap = new LinkedHashMap<>();

        try {
            // "Group"
            String pluginGroup = extension.getPluginGroup().getOrNull();
            if (pluginGroup == null || pluginGroup.trim().isEmpty()) {
                pluginGroup = getProjectGroupId().get();
                PluginLog.info("Used project group as pluginGroup \"" + pluginGroup + "\"!");
            }
            ManifestValidation.validateString("pluginGroup", pluginGroup);
            manifestMap.put("Group", pluginGroup);

            // "Name"
            String pluginName = extension.getPluginName().getOrNull();
            if (pluginName == null || pluginName.trim().isEmpty()) {
                pluginName = getProjectArtifactId().get();
                PluginLog.info("Used project name as pluginName \"" + pluginName + "\"!");
            }
            ManifestValidation.validateString("pluginName", pluginName);
            manifestMap.put("Name", pluginName);

            // "Version"
            String pluginVersion = extension.getPluginVersion().getOrNull();
            if (pluginVersion == null || pluginVersion.trim().isEmpty()) {
                pluginVersion = getProjectVersion().get();
                PluginLog.info("Used project version as pluginVersion \"" + pluginVersion + "\"!");
            }
            try {
                ManifestValidation.validateSemanticVersion("pluginVersion", pluginVersion);
            } catch (ValidationException e) {
                gracefulError(e);
                pluginVersion = FALLBACK_PLUGIN_VERSION;
                PluginLog.info("Setting fallback version \"" + pluginVersion + "\"");
            }
            manifestMap.put("Version", pluginVersion);

            // "ServerVersion"
            String serverVersion = extension.getServerVersion().getOrNull();
            if (serverVersion == null || serverVersion.trim().isEmpty()) {
                serverVersion = "*";
                PluginLog.info("Used wildcard as serverVersion \"" + serverVersion + "\"!");
            }
            try {
                ManifestValidation.validateSemanticVersionRange("serverVersion", serverVersion);
            } catch (ValidationException e) {
                gracefulError(e);
                serverVersion = FALLBACK_SERVER_VERSION;
                PluginLog.info("Setting fallback serverVersion \"" + serverVersion + "\"");
            }
            manifestMap.put("ServerVersion", serverVersion);

            // "Main"
            String mainClassPath = extension.getPluginMainClass().getOrNull();
            if (mainClassPath == null || mainClassPath.trim().isEmpty()) {
                List<String> mainClassCandidates = getMainClassCandidates().getOrNull();
                if (mainClassCandidates != null && mainClassCandidates.size() == 1) {
                    mainClassPath = mainClassCandidates.getFirst();
                }
                if (mainClassCandidates != null && mainClassCandidates.size() > 1) {
                    PluginLog.info("Found multiple pluginMainClass candidates. Please set your pluginMainClass manually.");
                }
            }
            ManifestValidation.validateFullyQualifiedClass("pluginMainClass", mainClassPath);
            manifestMap.put("Main", mainClassPath);

            // "Description"
            String pluginDescription = extension.getPluginDescription().getOrNull();
            if (pluginDescription != null && !pluginDescription.trim().isEmpty()) {
                try {
                    boolean isSeparatorAllowed = true;
                    boolean isWhitespaceAllowed = true;
                    ManifestValidation.validateString("pluginDescription", pluginDescription,
                        isSeparatorAllowed, isWhitespaceAllowed);
                    manifestMap.put("Description", pluginDescription);
                } catch (ValidationException e) {
                    gracefulError(e);
                }
            }

            // "Website"
            String pluginWebsite = extension.getPluginWebsite().getOrNull();
            if (pluginWebsite != null && !pluginWebsite.trim().isEmpty()) {
                try {
                    ManifestValidation.validateURI("pluginWebsite", pluginWebsite);
                    manifestMap.put("Website", pluginWebsite);
                } catch (ValidationException e) {
                    gracefulError(e);
                }
            }

            // "DisabledByDefault"
            boolean disabledByDefault = extension.getDisabledByDefault().getOrElse(false);
            if (disabledByDefault) {
                manifestMap.put("DisabledByDefault", true);
            }

            // "IncludesAssetPack"
            boolean includesAssetPack = extension.getIncludesAssetPack().getOrElse(false);
            if (includesAssetPack) {
                manifestMap.put("IncludesAssetPack", true);
            }

            // "Authors"
            ManifestAuthorsExtension authors = extension.getAuthors();
            List<ManifestAuthorExtension> authorListExtension = authors.getAuthorConfigList().get();
            List<Map<String, String>> authorList = new LinkedList<>();
            for (int i = 0; i < authorListExtension.size(); i++) {
                ManifestAuthorExtension author = authorListExtension.get(i);
                Map<String, String> authorObject = new LinkedHashMap<>();
                String key = "author[" + i + "]";

                // "Author": "Name"
                String authorName = author.getName().getOrNull();
                ManifestValidation.validateString(key + ".name", authorName);
                authorObject.put("Name", authorName);

                // "Author": "Email"
                String authorEmail = author.getEmail().getOrNull();
                if (authorEmail != null && !authorEmail.trim().isEmpty()) {
                    try {
                        ManifestValidation.validateEmailAddress(key + ".email", authorEmail);
                        authorObject.put("Email", authorEmail);
                    } catch (ValidationException e) {
                        // Email in author is optional
                        gracefulError(e);
                    }
                }

                // "Author": "Url"
                String authorUrl = author.getUrl().getOrNull();
                if (authorUrl != null && !authorUrl.trim().isEmpty()) {
                    try {
                        boolean isEmptyAllowed = true;
                        ManifestValidation.validateURI(key + ".url", authorUrl, isEmptyAllowed);
                        authorObject.put("Url", authorUrl);
                    } catch (ValidationException e) {
                        gracefulError(e);
                    }
                }
                authorList.add(authorObject);
            }
            // Default author
            if (authorList.isEmpty()) {
                Map<String, String> authorObject = new LinkedHashMap<>();
                String userName = getOSUserName().getOrNull();
                if (userName == null || userName.trim().isEmpty()) {
                    userName = pluginName + "-Author";
                    PluginLog.info("Used project name as authorName \"" + userName + "\".");
                } else {
                    PluginLog.info("Used system property \"user.name\" as authorName \"" + userName + "\".");
                }
                authorObject.put("Name", userName);
                authorList.add(authorObject);
            }
            manifestMap.put("Authors", authorList);

            ManifestPluginDependencyExtension dependencies = extension.getPluginDependencies();

            // "Dependencies"
            Map<String, String> requiredDependencies = dependencies.getRequiredDependencies().getOrNull();
            if (requiredDependencies != null && !requiredDependencies.isEmpty()) {
                ManifestValidation.validateDependencies("required", requiredDependencies);
                manifestMap.put("Dependencies", requiredDependencies);
            }
            // "OptionalDependencies"
            Map<String, String> optionalDependencies = dependencies.getOptionalDependencies().getOrNull();
            if (optionalDependencies != null && !optionalDependencies.isEmpty()) {
                ManifestValidation.validateDependencies("optional", optionalDependencies);
                manifestMap.put("OptionalDependencies", optionalDependencies);
            }
            // "LoadBefore"
            Map<String, String> loadBeforeDependencies = dependencies.getLoadBeforeDependencies().getOrNull();
            if (loadBeforeDependencies != null && !loadBeforeDependencies.isEmpty()) {
                ManifestValidation.validateDependencies("loadBefore", loadBeforeDependencies);
                manifestMap.put("LoadBefore", loadBeforeDependencies);
            }
        } catch (ValidationException e) {
            throw new InvalidUserDataException(e.getFormattedMessage());
        }

        String manifestJson = JsonOutput.toJson(manifestMap);

        if (!extension.getMinimizeJson().getOrElse(false)) {
            manifestJson = JsonOutput.prettyPrint(manifestJson);
        }

        File resourceDirectory = new File(getResourceDirectory().get());
        if (!resourceDirectory.exists()) {
            resourceDirectory.mkdir();
        }
        File manifestFile = new File(resourceDirectory, "manifest.json");

        Files.writeString(manifestFile.toPath(), manifestJson, StandardCharsets.UTF_8);

        PluginLog.info("");
        PluginLog.info("Successfully generated manifest.json at:");
        PluginLog.info(manifestFile.getAbsolutePath());
        PluginLog.info("");
    }

    private void gracefulError(ValidationException exception) {
        PluginLog.info("INVALID -> " + exception.getMessage());
    }
}
