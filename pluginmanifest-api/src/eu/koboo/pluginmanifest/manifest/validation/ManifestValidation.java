package eu.koboo.pluginmanifest.manifest.validation;

import eu.koboo.pluginmanifest.manifest.ManifestAuthor;
import eu.koboo.pluginmanifest.manifest.semver.SemVer;
import eu.koboo.pluginmanifest.manifest.semver.SemVerRange;
import lombok.experimental.UtilityClass;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@UtilityClass
public class ManifestValidation {

    public void validateRequired(List<ValidationResult> resultList, String value, String key) {
        if (value == null) {
            resultList.add(ValidationResult.of(key, null, "cannot be null"));
            return;
        }
        if (value.trim().isEmpty()) {
            resultList.add(ValidationResult.of(key, "", "cannot be empty"));
            return;
        }
    }

    public void validateCharacters(List<ValidationResult> resultList, String value, String key, char... allowedChars) {
        validateRequired(resultList, value, key);

        Set<Character> allowedSet = new HashSet<>();
        for (char allowedChar : allowedChars) {
            allowedSet.add(allowedChar);
        }

        char[] charArray = value.toCharArray();
        for (int i = 0; i < charArray.length; i++) {
            char c = charArray[i];
            boolean isAlphabetical = (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
            boolean isDigit = Character.isDigit(c);
            boolean isAllowedChar = allowedSet.contains(c);
            if (!isAlphabetical && !isDigit && !isAllowedChar) {
                resultList.add(ValidationResult.of(key, value, "contains illegal character: '" + c + "' at index " + i));
            }
        }
    }

    public void validatePluginIdentifier(List<ValidationResult> resultList, String pluginIdentifier) {
        String key = "pluginIdentifier";
        validateCharacters(resultList, pluginIdentifier, key, '-', ':');
        if (!pluginIdentifier.contains(":")) {
            resultList.add(ValidationResult.of(pluginIdentifier, key, "must contain ':' in format \"Group:Name\""));
            return;
        }
    }

    public void validateSemVer(List<ValidationResult> resultList, String semVerString) {
        String key = "pluginVersion";
        validateRequired(resultList, semVerString, key);
        SemVer.parseString(resultList, key, semVerString);
    }

    public void validateWebsite(List<ValidationResult> resultList, String website) {
        if (website == null || website.trim().isEmpty()) {
            return;
        }
        validateURI(resultList, website, "pluginWebsite");
    }

    public void validateSemVerRange(List<ValidationResult> resultList, String semVerRangeString, String pluginIdentifier) {
        validateRequired(resultList, semVerRangeString, pluginIdentifier);
        SemVerRange.parseString(resultList, pluginIdentifier, semVerRangeString);
    }

    @SuppressWarnings("all")
    public void validateURI(List<ValidationResult> resultList, String uriString, String key) {
        URI uri;
        try {
            uri = URI.create(uriString);
        } catch (IllegalArgumentException e) {
            resultList.add(ValidationResult.of(key, uriString, "is a malformed uri"));
            return;
        }
        String scheme = uri.getScheme();
        if (scheme == null || scheme.isEmpty() || (!scheme.equals("http") && !scheme.equals("https"))) {
            resultList.add(ValidationResult.of(key, uriString, "must start with 'http://' or 'https://'"));
            return;
        }
        String host = uri.getHost();
        if (host == null) {
            resultList.add(ValidationResult.of(key, uriString, "must have non-null host"));
            return;
        }
        if (host.isEmpty()) {
            resultList.add(ValidationResult.of(key, uriString, "must have a non-empty host"));
            return;
        }
        if (!host.contains(".")) {
            resultList.add(ValidationResult.of(key, uriString, "must have a host which contains at least one '.'"));
            return;
        }
    }

    public void validateAuthors(List<ValidationResult> resultList, List<ManifestAuthor> authorList) {
        String key = "authors";
        if (authorList == null) {
            resultList.add(ValidationResult.of(key, null, "must not be null"));
            return;
        }
        if (authorList.isEmpty()) {
            resultList.add(ValidationResult.of(key, "", "must have at least one author"));
        }
        for (int i = 0; i < authorList.size(); i++) {
            ManifestAuthor author = authorList.get(i);
            validateRequired(resultList, author.getName(), "authorName[" + i + "]");
            String authorEmail = author.getEmail();
            if (authorEmail != null && !authorEmail.trim().isEmpty()) {
                validateEmailAddress(resultList, authorEmail, "authorEmail[" + i + "]");
            }
            String authorUrl = author.getUrl();
            if (authorUrl != null && !authorUrl.trim().isEmpty()) {
                validateURI(resultList, authorUrl, "authorUrl[" + i + "]");
            }
        }
    }

    public void validateEmailAddress(List<ValidationResult> resultList, String emailAddress, String key) {
        int atIndex = emailAddress.indexOf('@');
        if (atIndex == -1) {
            resultList.add(ValidationResult.of(key, emailAddress, "must contain a '@'"));
            return;
        }
        if (atIndex != emailAddress.lastIndexOf('@')) {
            resultList.add(ValidationResult.of(key, emailAddress, "must contain only one '@'"));
            return;
        }
        String local = emailAddress.substring(0, atIndex);
        if (local.isEmpty()) {
            resultList.add(ValidationResult.of(key, emailAddress, "must contain a recipient/local"));
            return;
        }
        String domain = emailAddress.substring(atIndex + 1);
        if (domain.isEmpty()) {
            resultList.add(ValidationResult.of(key, emailAddress, "must have a domain"));
            return;
        }
        if (!domain.contains(".")) {
            resultList.add(ValidationResult.of(key, emailAddress, "must contain a domain with at least one '.'"));
            return;
        }
        if (domain.startsWith(".") || domain.endsWith(".")) {
            resultList.add(ValidationResult.of(key, emailAddress, "cannot contain a domain that starts or ends with a '.'"));
            return;
        }
    }

    public void validateDependencies(List<ValidationResult> resultList, Map<String, String> dependencies) {
        for (String pluginIdentifier : dependencies.keySet()) {
            validatePluginIdentifier(resultList, pluginIdentifier);
            String semVerRange = dependencies.get(pluginIdentifier);
            validateSemVerRange(resultList, semVerRange, pluginIdentifier);
        }
    }

    public void validateMainClass(List<ValidationResult> resultList, String className) {
        validateRequired(resultList, className, "pluginMainClass");

        if (className.startsWith(".") || className.endsWith(".")) {
            resultList.add(ValidationResult.of(className, "pluginMainClass", "cannot start or end with '.'"));
            return;
        }

        String[] parts = className.split("\\.");
        for (String part : parts) {
            if (part.isEmpty()) {
                resultList.add(ValidationResult.of(className, "pluginMainClass", "cannot contain consecutive '.'"));
                return;
            }

            char first = part.charAt(0);
            if (!Character.isJavaIdentifierStart(first)) {
                resultList.add(ValidationResult.of(className, "pluginMainClass", "does not start with a valid java class character"));
                return;
            }

            for (int i = 1; i < part.length(); i++) {
                char c = part.charAt(i);
                if (!Character.isJavaIdentifierPart(c)) {
                    resultList.add(ValidationResult.of(className, "pluginMainClass", "contains an invalid java class character"));
                    return;
                }
            }
        }
    }
}
