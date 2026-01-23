package eu.koboo.pluginmanifest.gradle.plugin;

import eu.koboo.pluginmanifest.gradle.plugin.extension.clientinstall.ClientInstallationExtension;
import eu.koboo.pluginmanifest.gradle.plugin.extension.manifest.JsonManifestExtension;
import eu.koboo.pluginmanifest.gradle.plugin.extension.serverruntime.ServerRuntimeExtension;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.gradle.api.Action;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

import javax.inject.Inject;

@Getter
@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
public abstract class PluginManifestExtension {

    JsonManifestExtension jsonManifestExtension;
    ServerRuntimeExtension serverRuntimeExtension;
    ClientInstallationExtension installationExtension;
    Property<Boolean> addClientServerDependency;

    @Inject
    public PluginManifestExtension(ObjectFactory objectFactory) {
        this.jsonManifestExtension = objectFactory.newInstance(JsonManifestExtension.class);
        this.serverRuntimeExtension = objectFactory.newInstance(ServerRuntimeExtension.class);
        this.installationExtension = objectFactory.newInstance(ClientInstallationExtension.class);
        this.addClientServerDependency = objectFactory.property(Boolean.class);
        this.addClientServerDependency.set(true);
    }

    public void manifestConfiguration(Action<JsonManifestExtension> action) {
        action.execute(jsonManifestExtension);
    }

    public void runtimeConfiguration(Action<ServerRuntimeExtension> action) {
        action.execute(serverRuntimeExtension);
    }

    public void clientInstallation(Action<ClientInstallationExtension> action) {
        action.execute(installationExtension);
    }
}
