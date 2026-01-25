package eu.koboo.pluginmanifest.gradle.plugin.utils;

import eu.koboo.pluginmanifest.gradle.plugin.PluginManifestExtension;
import eu.koboo.pluginmanifest.gradle.plugin.extension.manifest.JsonManifestExtension;
import eu.koboo.pluginmanifest.gradle.plugin.extension.manifest.ManifestAuthorExtension;
import eu.koboo.pluginmanifest.gradle.plugin.extension.manifest.ManifestAuthorsExtension;
import eu.koboo.pluginmanifest.gradle.plugin.extension.manifest.ManifestPluginDependencyExtension;
import eu.koboo.pluginmanifest.gradle.plugin.tasks.validation.ManifestValidation;
import eu.koboo.pluginmanifest.gradle.plugin.tasks.validation.ValidationException;
import lombok.experimental.UtilityClass;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@UtilityClass
public class ProviderUtils {


    public Provider<String> createPluginGroupProvider(Project project) {
        return project.provider(() -> project.getGroup().toString());
    }

    public Provider<String> createPluginNameProvider(Project project) {
        return project.provider(project::getName);
    }

    public Provider<String> createPluginVersionProvider(Project project) {
        return project.provider(() -> project.getVersion().toString());
    }

    public Provider<Boolean> createHasResourcesProvider(Project project) {
        return project.provider(() -> JavaSourceUtils.hasResources(project));
    }

    public Provider<String> createPluginMainClassCandidateProvider(Project project) {
        return project.provider(() -> {
            List<String> mainClassCandidates = JavaSourceUtils.getMainClassCandidates(project);
            if (mainClassCandidates == null || mainClassCandidates.isEmpty()) {
                PluginLog.info("Found no pluginMainClass candidates. Please set your pluginMainClass manually.");
                return null;
            }
            if (mainClassCandidates.size() != 1) {
                PluginLog.info("Found multiple pluginMainClass candidates. Please set your pluginMainClass manually.");
                return null;
            }
            return mainClassCandidates.getFirst();
        });
    }

    public Provider<Map<String, Object>> createManifestProvider(Project project) {
        return project.provider(() -> {
            PluginManifestExtension pluginManifestExt = project.getExtensions().getByType(PluginManifestExtension.class);
            JsonManifestExtension extension = pluginManifestExt.getJsonManifestExtension();

            Map<String, Object> manifestMap = new LinkedHashMap<>();

            try {
                // "Group"
                String pluginGroup = extension.getPluginGroup().get();
                ManifestValidation.validateString("pluginGroup", pluginGroup);
                manifestMap.put("Group", pluginGroup);

                // "Name"
                String pluginName = extension.getPluginName().get();
                ManifestValidation.validateString("pluginName", pluginName);
                manifestMap.put("Name", pluginName);

                // "Version"
                String pluginVersion = extension.getPluginVersion().get();
                ManifestValidation.validateSemanticVersion("pluginVersion", pluginVersion);
                manifestMap.put("Version", pluginVersion);

                // "ServerVersion"
                String serverVersion = extension.getServerVersion().get();
                ManifestValidation.validateSemanticVersionRange("serverVersion", serverVersion);
                manifestMap.put("ServerVersion", serverVersion);

                // "Main"
                String mainClassPath = extension.getPluginMainClass().get();
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
                boolean includesAssetPack = extension.getIncludesAssetPack().get();
                if (includesAssetPack) {
                    manifestMap.put("IncludesAssetPack", true);
                }

                // "Authors"
                ManifestAuthorsExtension authors = extension.getManifestAuthors();
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
                if (!authorList.isEmpty()) {
                    manifestMap.put("Authors", authorList);
                }

                ManifestPluginDependencyExtension dependencies = extension.getManifestPluginDependencies();

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

            return manifestMap;
        });
    }

    private void gracefulError(ValidationException exception) {
        PluginLog.info("INVALID -> " + exception.getMessage());
    }

}
