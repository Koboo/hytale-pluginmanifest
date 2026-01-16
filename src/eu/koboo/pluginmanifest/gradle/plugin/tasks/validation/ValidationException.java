package eu.koboo.pluginmanifest.gradle.plugin.tasks.validation;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ValidationException extends Exception {
    String key;
    String value;
    String message;

    public String getFormattedMessage() {
        return key + " " + message + " -> \"" + value + "\"";
    }
}