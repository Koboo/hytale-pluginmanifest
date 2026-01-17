package eu.koboo.pluginmanifest.gradle.plugin.extension.serverdependency;

import eu.koboo.pluginmanifest.gradle.plugin.utils.PluginLog;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.gradle.api.GradleException;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.io.File;
import java.util.LinkedList;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public abstract class ServerRuntimeExtension {

    @Input
    @Optional
    Property<String> runtimeDirectory;

    @Input
    Property<Boolean> acceptEarlyPlugins;
    @Input
    Property<Boolean> allowOp;
    @Input
    Property<String> bindAddress;
    @Input
    Property<Boolean> enableNativeAccess;
    @Input
    ListProperty<String> jvmArguments;
    @Input
    ListProperty<String> serverArguments;

    @Inject
    public ServerRuntimeExtension(ObjectFactory objectFactory) {
        runtimeDirectory = objectFactory.property(String.class);

        acceptEarlyPlugins = objectFactory.property(Boolean.class);
        acceptEarlyPlugins.convention(true);

        allowOp = objectFactory.property(Boolean.class);
        allowOp.convention(true);

        bindAddress = objectFactory.property(String.class);
        bindAddress.convention("0.0.0.0:5520");

        enableNativeAccess = objectFactory.property(Boolean.class);
        enableNativeAccess.convention(true);

        jvmArguments = objectFactory.listProperty(String.class);
        jvmArguments.convention(new LinkedList<>());

        serverArguments = objectFactory.listProperty(String.class);
        serverArguments.convention(new LinkedList<>());
    }

    public @NotNull File resolveRuntimeDirectory() {
        String runtimeDirectoryPath = getRuntimeDirectory().getOrNull();
        if (runtimeDirectoryPath == null || runtimeDirectoryPath.trim().isEmpty()) {
            throw new GradleException("Can't resolve runtimeDirectory, no path set!");
        }
        File runtimeDirectory = new File(runtimeDirectoryPath);
        if (runtimeDirectory.exists() && !runtimeDirectory.isDirectory()) {
            throw new GradleException("Can't resolve runtimeDirectory, path is not a directory: " + runtimeDirectory.getAbsolutePath());
        }
        if (!runtimeDirectory.exists()) {
            runtimeDirectory.mkdirs();
            PluginLog.info("Created serverRuntimeDirectory: " + runtimeDirectory.getAbsolutePath());
        }
        return runtimeDirectory;
    }

    public @NotNull File resolveRuntimeServerJarFile() {
        return new File(resolveRuntimeDirectory(), "HytaleServer.jar");
    }

    public @NotNull File resolveRuntimeAOTFile() {
        return new File(resolveRuntimeDirectory(), "HytaleServer.aot");
    }

    public @NotNull File resolveRuntimeAssetsFile() {
        return new File(resolveRuntimeDirectory(), "Assets.zip");
    }

    public @NotNull File resolveRuntimeModDirectory() {
        return new File(resolveRuntimeDirectory(), "mods/");
    }
}
