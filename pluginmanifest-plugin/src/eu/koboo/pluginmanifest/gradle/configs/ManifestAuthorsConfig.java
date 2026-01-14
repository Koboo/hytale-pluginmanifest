package eu.koboo.pluginmanifest.gradle.configs;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.gradle.api.Action;

import java.util.LinkedList;
import java.util.List;

@Getter(AccessLevel.PROTECTED)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ManifestAuthorsConfig {

    List<ManifestAuthorConfig> authorConfigList = new LinkedList<>();

    public void author(Action<ManifestAuthorConfig> action) {
        ManifestAuthorConfig config = new ManifestAuthorConfig();
        action.execute(config);
        authorConfigList.add(config);
    }
}
