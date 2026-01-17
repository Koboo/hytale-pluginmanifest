package eu.koboo.pluginmanifest.gradle.plugin.extension.serverdependency;

import eu.koboo.pluginmanifest.gradle.plugin.PluginLog;
import eu.koboo.pluginmanifest.gradle.plugin.extension.AuthMode;
import eu.koboo.pluginmanifest.gradle.plugin.extension.Patchline;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.gradle.api.GradleException;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.io.File;
import java.util.LinkedList;
import java.util.Locale;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public abstract class ServerRuntimeExtension {
    private static final String LATEST_DIRECTORY = "install/PATCHLINE/package/game/latest/";

    @Input
    Property<Patchline> patchline;
    @Input
    Property<String> clientInstallDirectory;

    @Input
    Property<Boolean> applyServerDependency;

    @Input
    @Optional
    Property<String> serverRuntimePath;

    @Input
    Property<Boolean> disableSentry;
    @Input
    Property<Boolean> acceptEarlyPlugins;
    @Input
    Property<Boolean> allowOp;
    @Input
    Property<AuthMode> authMode;
    @Input
    Property<String> bindAddress;
    @Input
    ListProperty<String> jvmArguments;
    @Input
    ListProperty<String> serverArguments;

    @Inject
    public ServerRuntimeExtension(ObjectFactory objectFactory, ProviderFactory providerFactory) {
        patchline = objectFactory.property(Patchline.class);
        patchline.set(Patchline.RELEASE);
        clientInstallDirectory = objectFactory.property(String.class);
        clientInstallDirectory.convention(
            providerFactory.provider(ServerRuntimeExtension::resolveDefaultClientDirectory)
        );

        applyServerDependency = objectFactory.property(Boolean.class);
        applyServerDependency.set(true);

        serverRuntimePath = objectFactory.property(String.class);

        disableSentry = objectFactory.property(Boolean.class);
        disableSentry.convention(false);

        acceptEarlyPlugins = objectFactory.property(Boolean.class);
        acceptEarlyPlugins.convention(false);

        allowOp = objectFactory.property(Boolean.class);
        allowOp.convention(true);

        authMode = objectFactory.property(AuthMode.class);
        authMode.convention(AuthMode.OFFLINE);

        bindAddress = objectFactory.property(String.class);
        bindAddress.convention("0.0.0.0:5520");

        jvmArguments = objectFactory.listProperty(String.class);
        jvmArguments.convention(new LinkedList<>());

        serverArguments = objectFactory.listProperty(String.class);
        serverArguments.convention(new LinkedList<>());
    }

    public @NotNull File resolveRuntimeDirectory() {
        String runtimeDirectoryPath = getServerRuntimePath().getOrNull();
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

    public boolean hasRuntimeDirectory() {
        String runtimeDirectoryPath = getServerRuntimePath().getOrNull();
        if (runtimeDirectoryPath == null || runtimeDirectoryPath.trim().isEmpty()) {
            return false;
        }
        File runtimeDirectory = new File(runtimeDirectoryPath);
        if (runtimeDirectory.exists() && !runtimeDirectory.isDirectory()) {
            return false;
        }
        return true;
    }

    // APPDATA/Hytale/
    public @NotNull File resolveClientLatestDirectory() {
        String clientRootDirectoryPath = getClientInstallDirectory().getOrNull();
        if (clientRootDirectoryPath == null || clientRootDirectoryPath.trim().isEmpty()) {
            throw new GradleException("Can't resolve clientRootDirectory, because it can't be null or empty!");
        }
        File clientRootDirectory = new File(clientRootDirectoryPath);
        if (!clientRootDirectory.exists()) {
            throw new GradleException("Can't resolve clientRootDirectory, because it doesn't exists: " + clientRootDirectory.getAbsolutePath());
        }
        // APPDATA/Hytale/install/PATCHLINE/package/game/latest/
        Patchline patchline = getPatchline().getOrNull();
        if (patchline == null) {
            throw new GradleException("patchline can't be null!");
        }
        String patchlineString = patchline.name().toLowerCase(Locale.ROOT);
        String latestDirectoryPath = LATEST_DIRECTORY.replace("PATCHLINE", patchlineString);
        return new File(clientRootDirectory, latestDirectoryPath);
    }

    public @NotNull File resolveClientServerJarFile() {
        return new File(resolveClientLatestDirectory(), "Server/HytaleServer.jar");
    }

    public @NotNull File resolveClientAOTFile() {
        return new File(resolveClientLatestDirectory(), "Server/HytaleServer.aot");
    }

    public @NotNull File resolveClientAssetsFile() {
        return new File(resolveClientLatestDirectory(), "Assets.zip");
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

    public static @NotNull String resolveDefaultClientDirectory() {
        String appDataDirectory = resolveAppDataDirectory();
        if (appDataDirectory == null || appDataDirectory.trim().isEmpty()) {
            throw new GradleException("Couldn't find client-installation path!");
        }
        if (!appDataDirectory.endsWith("/")) {
            appDataDirectory += "/";
        }
        return appDataDirectory + "Hytale/";
    }

    private static @Nullable String resolveAppDataDirectory() {
        String osName = System.getProperty("os.name");
        if (osName == null || osName.trim().isEmpty()) {
            throw new GradleException("Couldn't find operating system name by system property \"os.name\"");
        }
        osName = osName.toLowerCase(Locale.ROOT);
        if (osName.startsWith("win")) {
            String appDataDirectory = System.getenv("APPDATA");
            if (appDataDirectory == null || appDataDirectory.trim().isEmpty()) {
                String userHome = System.getProperty("user.home");
                if (userHome != null && !userHome.trim().isEmpty()) {
                    appDataDirectory = userHome + "/AppData/Roaming";
                }
            }
            return appDataDirectory;
        }
        if (osName.startsWith("mac")) {
            String userHome = System.getProperty("user.home");
            if (userHome == null || userHome.trim().isEmpty()) {
                return null;
            }
            return userHome + "/Library/Application Support/";
        }
        if (osName.startsWith("linux")) {
            String dataHome = System.getenv("XDG_DATA_HOME");
            if (dataHome == null || dataHome.trim().isEmpty()) {
                dataHome = "/.local/share/";
            }
            return dataHome;
        }
        throw new GradleException("Your operating system \"" + osName + "\" is not supported!");
    }
}
