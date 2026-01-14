package eu.koboo.pluginmanifest.api.validation;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InvalidPluginManifestException extends RuntimeException {
    List<ValidationResult> resultList;

    public String buildMessage() {
        Map<String, String> errorKeyMap = new LinkedHashMap<>();
        for (ValidationResult validationResult : resultList) {
            String key = validationResult.key();
            errorKeyMap.put(key, key + " " + validationResult.message() + " -> " + validationResult.value());
        }
        StringBuilder messageBuilder = new StringBuilder();
        for (String key : errorKeyMap.keySet()) {
            String message = errorKeyMap.get(key);
            messageBuilder.append(" - ").append(message).append(System.lineSeparator());
        }
        return messageBuilder.toString();
    }
}
