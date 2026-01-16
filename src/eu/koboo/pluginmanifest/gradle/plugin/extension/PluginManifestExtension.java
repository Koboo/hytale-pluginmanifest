package eu.koboo.pluginmanifest.gradle.plugin.extension;

import eu.koboo.pluginmanifest.gradle.plugin.extension.manifest.ManifestExtension;
import eu.koboo.pluginmanifest.gradle.plugin.extension.serverdependency.ServerRuntimeExtension;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.gradle.api.Action;
import org.gradle.api.model.ObjectFactory;

import javax.inject.Inject;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public abstract class PluginManifestExtension {

    ManifestExtension manifestExtension;
    ServerRuntimeExtension runtimeExtension;

    @Inject
    public PluginManifestExtension(ObjectFactory objectFactory) {
        this.manifestExtension = objectFactory.newInstance(ManifestExtension.class);
        this.runtimeExtension = objectFactory.newInstance(ServerRuntimeExtension.class);
    }

    public void manifestConfiguration(Action<ManifestExtension> action) {
        action.execute(manifestExtension);
    }

    public void runtimeConfiguration(Action<ServerRuntimeExtension> action) {
        action.execute(runtimeExtension);
    }
}
