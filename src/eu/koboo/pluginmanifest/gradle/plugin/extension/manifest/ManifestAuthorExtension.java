package eu.koboo.pluginmanifest.gradle.plugin.extension.manifest;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;

import javax.inject.Inject;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public abstract class ManifestAuthorExtension {

    @Input
    Property<String> name;
    @Input
    @Optional
    Property<String> email;
    @Input
    @Optional
    Property<String> url;

    @Inject
    public ManifestAuthorExtension(ObjectFactory objectFactory) {
        name = objectFactory.property(String.class);
        email = objectFactory.property(String.class);
        url = objectFactory.property(String.class);
    }
}
