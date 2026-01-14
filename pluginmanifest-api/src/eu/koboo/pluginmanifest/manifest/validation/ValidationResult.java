package eu.koboo.pluginmanifest.manifest.validation;

public record ValidationResult(String key, String value, String message) {

    public static ValidationResult of(String key, String value, String message) {
        return new ValidationResult(key, value, message);
    }
}
