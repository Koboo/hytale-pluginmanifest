package eu.koboo.pluginmanifest.gradle.plugin.extension.serverruntime;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.gradle.api.GradleException;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

import javax.inject.Inject;
import java.io.File;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public abstract class ServerRuntimeExtension {

    Property<String> runtimeDirectory;
    Property<Boolean> copyPluginToRuntime;

    Property<Boolean> allowOp;
    Property<String> bindAddress;
    ListProperty<String> jvmArguments;
    ListProperty<String> serverArguments;

    @Inject
    public ServerRuntimeExtension(ObjectFactory objectFactory) {
        runtimeDirectory = objectFactory.property(String.class);
        copyPluginToRuntime = objectFactory.property(Boolean.class);
        allowOp = objectFactory.property(Boolean.class);
        bindAddress = objectFactory.property(String.class);
        jvmArguments = objectFactory.listProperty(String.class);
        serverArguments = objectFactory.listProperty(String.class);
    }

    public File resolveRuntimeDirectory() {
        String runtimeDirectoryPath = getRuntimeDirectory().getOrNull();
        if (runtimeDirectoryPath == null || runtimeDirectoryPath.trim().isEmpty()) {
            throw new GradleException("Can't resolve runtimeDirectory, no path set!");
        }
        File runtimeDirectory = new File(runtimeDirectoryPath);
        if (runtimeDirectory.exists() && !runtimeDirectory.isDirectory()) {
            throw new GradleException("Can't resolve runtimeDirectory, path is not a directory: " + runtimeDirectory.getAbsolutePath());
        }
        return runtimeDirectory;
    }
}
