package eu.koboo.pluginmanifest.gradle.plugin.tasks.validation;

import eu.koboo.pluginmanifest.gradle.plugin.tasks.validation.semver.SemVer;
import eu.koboo.pluginmanifest.gradle.plugin.tasks.validation.semver.SemVerRange;
import lombok.experimental.UtilityClass;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@UtilityClass
public class ManifestValidation {

    public void validateString(String key, String value) throws ValidationException {
        validateString(key, value, false, false);
    }

    public void validateString(String key, String value, boolean isSeparatorAllowed, boolean isWhiteSpaceAllowed) throws ValidationException {
        if (value == null) {
            throw new ValidationException(key, null, "cannot be null");
        }
        value = value.trim();
        if (value.isEmpty()) {
            throw new ValidationException(key, value, "cannot be empty");
        }
        char[] charArray = value.toCharArray();
        for (char currentChar : charArray) {
            if (Character.isISOControl(currentChar)) {
                throw new ValidationException(key, value, "contains iso-control character");
            }
            if (!isWhiteSpaceAllowed && Character.isWhitespace(currentChar)) {
                throw new ValidationException(key, value, "contains whitespace character");
            }
            if (!isSeparatorAllowed && currentChar == ':') {
                throw new ValidationException(key, value, "contains ':' character");
            }
        }
    }

    public void validateSemanticVersion(String key, String versionString) throws ValidationException {
        if (versionString == null) {
            throw new ValidationException(key, null, "cannot be null");
        }
        versionString = versionString.trim();
        if (versionString.isEmpty()) {
            throw new ValidationException(key, versionString, "cannot be empty");
        }
        SemVer.parseString(key, versionString);
    }

    public void validateSemanticVersionRange(String key, String semVerRangeString) throws ValidationException {
        if (semVerRangeString == null) {
            throw new IllegalArgumentException("cannot be null");
        }
        semVerRangeString = semVerRangeString.trim();
        if (semVerRangeString.isEmpty()) {
            throw new IllegalArgumentException("cannot be empty");
        }
        SemVerRange.parseString(key, semVerRangeString);
    }

    @SuppressWarnings("all")
    public void validateURI(String key, String uriString) throws ValidationException {
        validateURI(key, uriString, false);
    }

    public void validateURI(String key, String uriString, boolean isEmptyAllowed) throws ValidationException {
        if (uriString == null) {
            if (isEmptyAllowed) {
                return;
            }
            throw new ValidationException(key, null, "cannot be null");
        }
        uriString = uriString.trim();
        if (uriString.isEmpty()) {
            if (isEmptyAllowed) {
                return;
            }
            throw new ValidationException(key, uriString, "cannot be empty");
        }
        URI uri;
        try {
            uri = URI.create(uriString);
        } catch (IllegalArgumentException e) {
            throw new ValidationException(key, uriString, "is a malformed uri");
        }
        String scheme = uri.getScheme();
        if (scheme == null || scheme.isEmpty() || (!scheme.equals("http") && !scheme.equals("https"))) {
            throw new ValidationException(key, uriString, "must start with 'http://' or 'https://'");
        }
        String host = uri.getHost();
        if (host == null) {
            throw new ValidationException(key, uriString, "must have non-null host");
        }
        if (host.isEmpty()) {
            throw new ValidationException(key, uriString, "must have a non-empty host");
        }
        if (!host.contains(".")) {
            throw new ValidationException(key, uriString, "must have a host which contains at least one '.'");
        }
    }

    public void validateEmailAddress(String key, String emailAddress) throws ValidationException {
        if (emailAddress == null) {
            return;
        }
        emailAddress = emailAddress.trim();
        if (emailAddress.isEmpty()) {
            return;
        }
        int atIndex = emailAddress.indexOf('@');
        if (atIndex == -1) {
            throw new ValidationException(key, emailAddress, "must contain a '@'");
        }
        if (atIndex != emailAddress.lastIndexOf('@')) {
            throw new ValidationException(key, emailAddress, "must contain only one '@'");
        }
        String local = emailAddress.substring(0, atIndex);
        if (local.isEmpty()) {
            throw new ValidationException(key, emailAddress, "must contain a recipient/local");
        }
        String domain = emailAddress.substring(atIndex + 1);
        if (domain.isEmpty()) {
            throw new ValidationException(key, emailAddress, "must have a domain");
        }
        if (!domain.contains(".")) {
            throw new ValidationException(key, emailAddress, "must contain a domain with at least one '.'");
        }
        if (domain.startsWith(".") || domain.endsWith(".")) {
            throw new ValidationException(key, emailAddress, "cannot contain a domain that starts or ends with a '.'");
        }
    }

    public void validateDependencies(String type, Map<String, String> dependencyMap) throws ValidationException {
        List<String> pluginIdList = new ArrayList<>(dependencyMap.keySet());
        for (int i = 0; i < pluginIdList.size(); i++) {
            String pluginId = pluginIdList.get(i);
            String key = "pluginDependency[index=" + i + ", type=" + type + "]";
            ManifestValidation.validateString(key + ".pluginId", pluginId, true, false);
            if (!pluginId.contains(":")) {
                throw new ValidationException(key, pluginId, "doesn't contain a ':'");
            }
            String[] pluginIdParts = pluginId.split(":");
            int partsLength = pluginIdParts.length;
            if (partsLength != 2) {
                throw new ValidationException(key, pluginId, "has " + partsLength + " but can only contain 2");
            }
            String pluginGroup = pluginIdParts[0];
            ManifestValidation.validateString(key + ".pluginId[pluginGroup]", pluginGroup);
            String pluginName = pluginIdParts[1];
            ManifestValidation.validateString(key + ".pluginId[pluginName]", pluginName);
            String semVerRange = dependencyMap.get(pluginId);
            ManifestValidation.validateSemanticVersionRange(key + ".versionRange", semVerRange);
        }
    }

    public void validateFullyQualifiedClass(String key, String className) throws ValidationException {
        if (className == null) {
            throw new ValidationException(key, null, "must not be null");
        }
        if (className.startsWith(".") || className.endsWith(".")) {
            throw new ValidationException(key, className, "cannot start or end with '.'");
        }

        String[] parts = className.split("\\.");
        for (String part : parts) {
            if (part.isEmpty()) {
                throw new ValidationException(key, className, "cannot contain consecutive '.'");
            }

            char first = part.charAt(0);
            if (!Character.isJavaIdentifierStart(first)) {
                throw new ValidationException(key, className, "does not start with a valid java class character");
            }

            for (int i = 1; i < part.length(); i++) {
                char c = part.charAt(i);
                if (!Character.isJavaIdentifierPart(c)) {
                    throw new ValidationException(key, className, "contains an invalid java class character");
                }
            }
        }
    }
}
