package eu.koboo.pluginmanifest.gradle.plugin;

import eu.koboo.pluginmanifest.gradle.plugin.extension.clientinstall.ClientInstallationExtension;
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
    ServerRuntimeExtension serverRuntimeExtension;
    ClientInstallationExtension installationExtension;

    @Inject
    public PluginManifestExtension(ObjectFactory objectFactory) {
        this.manifestExtension = objectFactory.newInstance(ManifestExtension.class);
        this.serverRuntimeExtension = objectFactory.newInstance(ServerRuntimeExtension.class);
        this.installationExtension = objectFactory.newInstance(ClientInstallationExtension.class);
    }

    public void manifestConfiguration(Action<ManifestExtension> action) {
        action.execute(manifestExtension);
    }

    public void runtimeConfiguration(Action<ServerRuntimeExtension> action) {
        action.execute(serverRuntimeExtension);
    }

    public void clientInstallation(Action<ClientInstallationExtension> action) {
        action.execute(installationExtension);
    }
}
