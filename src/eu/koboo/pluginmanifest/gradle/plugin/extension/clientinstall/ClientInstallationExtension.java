package eu.koboo.pluginmanifest.gradle.plugin.extension.clientinstall;

import eu.koboo.pluginmanifest.gradle.plugin.extension.Patchline;
import eu.koboo.pluginmanifest.gradle.plugin.utils.PluginLog;
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
public abstract class ClientInstallationExtension {
    private static final String LATEST_DIRECTORY = "install/PATCHLINE/package/game/latest/";

    @Input
    Property<Patchline> patchline;
    @Input
    Property<String> clientInstallDirectory;

    @Inject
    public ClientInstallationExtension(ObjectFactory objectFactory, ProviderFactory providerFactory) {
        patchline = objectFactory.property(Patchline.class);
        patchline.set(Patchline.RELEASE);
        clientInstallDirectory = objectFactory.property(String.class);
        clientInstallDirectory.convention(
            providerFactory.provider(ClientInstallationExtension::resolveDefaultClientDirectory)
        );
    }

    public @NotNull File resolveClientInstallDirectory() {
        String clientRootDirectoryPath = getClientInstallDirectory().getOrNull();
        if (clientRootDirectoryPath == null || clientRootDirectoryPath.trim().isEmpty()) {
            throw new GradleException("Can't resolve clientRootDirectory, because it can't be null or empty!");
        }
        File clientRootDirectory = new File(clientRootDirectoryPath);
        if (!clientRootDirectory.exists()) {
            throw new GradleException("Can't resolve clientRootDirectory, because it doesn't exists: " + clientRootDirectory.getAbsolutePath());
        }
        return clientRootDirectory;
    }

    // APPDATA/Hytale/
    public @NotNull File resolveClientLatestDirectory() {
        File clientRootDirectory = resolveClientInstallDirectory();
        // APPDATA/Hytale/install/PATCHLINE/package/game/latest/
        Patchline patchline = getPatchline().getOrNull();
        if (patchline == null) {
            throw new GradleException("patchline can't be null!");
        }
        String patchlineString = patchline.name().toLowerCase(Locale.ROOT);
        String latestDirectoryPath = LATEST_DIRECTORY.replace("PATCHLINE", patchlineString);
        return new File(clientRootDirectory, latestDirectoryPath);
    }

    public @NotNull File resolveClientServerDirectory() {
        return new File(resolveClientLatestDirectory(), "Server/");
    }

    public @NotNull File resolveClientServerJarFile() {
        return new File(resolveClientServerDirectory(), "HytaleServer.jar");
    }

    public @NotNull File resolveClientServerSourcesFile() {
        return new File(resolveClientServerDirectory(), "HytaleServer-sources.jar");
    }

    public @NotNull File resolveClientAOTFile() {
        return new File(resolveClientServerDirectory(), "HytaleServer.aot");
    }

    public @NotNull File resolveClientAssetsFile() {
        return new File(resolveClientLatestDirectory(), "Assets.zip");
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
        String userHome = System.getProperty("user.home");
        osName = osName.toLowerCase(Locale.ROOT);
        if (osName.startsWith("win")) {
            String appDataDirectory = System.getenv("APPDATA");
            if (appDataDirectory != null && appDataDirectory.trim().isEmpty()) {
                return appDataDirectory;
            }
            if (userHome != null && !userHome.trim().isEmpty()) {
                return userHome + "/AppData/Roaming";
            }
            return null;
        }
        if (osName.startsWith("mac")) {
            if (userHome != null && !userHome.trim().isEmpty()) {
                return userHome + "/Library/Application Support/";
            }
            return null;
        }
        if (osName.startsWith("linux")) {
            if (userHome != null && userHome.trim().isEmpty()) {
                return userHome + "/.var/app/com.hypixel.HytaleLauncher/data/";
            }
            String dataHome = System.getenv("XDG_DATA_HOME");
            if (dataHome == null || dataHome.trim().isEmpty()) {
                return dataHome + "/.local/share/";
            }
            return null;
        }
        throw new GradleException("Your operating system \"" + osName + "\" is not supported!");
    }
}
