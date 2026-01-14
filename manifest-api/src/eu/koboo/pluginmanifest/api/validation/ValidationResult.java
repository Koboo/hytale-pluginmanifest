package eu.koboo.pluginmanifest.api.validation;

public record ValidationResult(String key, String value, String message) {

    public static ValidationResult of(String key, String value, String message) {
        return new ValidationResult(key, value, message);
    }
}
