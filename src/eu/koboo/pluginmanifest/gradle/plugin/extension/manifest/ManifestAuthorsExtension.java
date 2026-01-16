package eu.koboo.pluginmanifest.gradle.plugin.extension.manifest;

import org.gradle.api.Action;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.Nested;

import javax.inject.Inject;
import java.util.LinkedList;

public abstract class ManifestAuthorsExtension {

    @Inject
    public ManifestAuthorsExtension() {
        getAuthorConfigList().convention(new LinkedList<>());
    }

    public void author(Action<ManifestAuthorExtension> action) {
        ManifestAuthorExtension authorExtension = getObjects().newInstance(ManifestAuthorExtension.class);
        action.execute(authorExtension);
        getAuthorConfigList().add(authorExtension);
    }

    @Nested
    public abstract ListProperty<ManifestAuthorExtension> getAuthorConfigList();

    @Inject
    public abstract ObjectFactory getObjects();
}
