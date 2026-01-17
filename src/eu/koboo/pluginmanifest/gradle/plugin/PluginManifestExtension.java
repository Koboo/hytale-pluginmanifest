package eu.koboo.pluginmanifest.gradle.plugin;

import eu.koboo.pluginmanifest.gradle.plugin.extension.manifest.ManifestExtension;
import eu.koboo.pluginmanifest.gradle.plugin.extension.serverdependency.ServerRuntimeExtension;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.gradle.api.Action;
import org.gradle.api.model.ObjectFactory;

import javax.inject.Inject;

@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
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
