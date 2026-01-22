package eu.koboo.pluginmanifest.gradle.plugin.extension.manifest;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

import javax.inject.Inject;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public abstract class ManifestAuthorExtension {

    Property<String> name;
    Property<String> email;
    Property<String> url;

    @Inject
    public ManifestAuthorExtension(ObjectFactory objectFactory) {
        name = objectFactory.property(String.class);
        email = objectFactory.property(String.class);
        url = objectFactory.property(String.class);
    }
}
