package eu.koboo.pluginmanifest.gradle.configs;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class ManifestAuthorConfig {

    String name;
    String email;
    String url;

    public String authorName() {
        return name;
    }

    public void authorName(String name) {
        this.name = name;
    }

    public String authorEmail() {
        return email;
    }

    public void authorEmail(String email) {
        this.email = email;
    }

    public String authorUrl() {
        return url;
    }

    public void authorUrl(String url) {
        this.url = url;
    }
}
