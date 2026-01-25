package eu.koboo.pluginmanifest.gradle.plugin.extension.clientinstall;

import eu.koboo.pluginmanifest.gradle.plugin.extension.Patchline;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFile;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public abstract class ClientInstallationExtension {
    private static final String LATEST_DIRECTORY = "install/PATCHLINE/package/game/latest/";

    Property<Patchline> patchline;
    Property<String> clientInstallDirectory;

    @Inject
    public ClientInstallationExtension() {
        patchline = getObjectFactory().property(Patchline.class);
        clientInstallDirectory = getObjectFactory().property(String.class);
    }

    @Inject
    public abstract ProviderFactory getProviderFactory();

    @Inject
    public abstract ObjectFactory getObjectFactory();

    public Provider<String> resolvePatchlineProvider() {
        return getProviderFactory().provider(() -> {
            Patchline patchline = getPatchline().getOrNull();
            if (patchline == null) {
                throw new InvalidUserDataException("patchline can't be null!");
            }
            return patchline.name()
                .toLowerCase(Locale.ROOT)
                .replace('_', '-');
        });
    }

    private Directory clientDirectory() {
        DirectoryProperty directoryProperty = getObjectFactory().directoryProperty();
        directoryProperty.set(new File(clientInstallDirectory.get()));
        return directoryProperty.get();
    }

    public Provider<String> provideClientPath(String filePath) {
        return getProviderFactory().provider(() -> {
                File clientDirectory = clientDirectory().getAsFile();
                File clientFile = new File(clientDirectory, getLatestDirectoryPath() + filePath);
                return clientFile.getAbsolutePath();
            }
        );
    }

    public Provider<RegularFile> provideClientFile(String filePath) {
        return getProviderFactory().provider(() ->
            clientDirectory().file(getLatestDirectoryPath() + filePath)
        );
    }

    public Provider<Directory> provideClientDirectory(String directoryPath) {
        return getProviderFactory().provider(() ->
            clientDirectory().dir(getLatestDirectoryPath() + directoryPath)
        );
    }

    public String getLatestDirectoryPath() {
        // install/PATCHLINE/package/game/latest/
        String patchlineName = resolvePatchlineProvider().get();
        return LATEST_DIRECTORY.replace("PATCHLINE", patchlineName);
    }

    public Provider<String> createDefaultAppDataProvider() {
        return getProviderFactory().provider(() -> {
            List<String> appDataPathList = getAppDataPathList();
            List<String> modifiedPathList = new ArrayList<>();
            File defaultClientDirectory = null;
            for (String directoryPath : appDataPathList) {
                directoryPath = directoryPath.trim();
                if (!directoryPath.endsWith("/")) {
                    directoryPath += "/";
                }
                directoryPath += "Hytale/";
                modifiedPathList.add(directoryPath);
                File clientHome = new File(directoryPath);
                if (!clientHome.exists()) {
                    continue;
                }
                if (!clientHome.isDirectory()) {
                    continue;
                }
                File[] files = clientHome.listFiles();
                if (files == null || files.length == 0) {
                    continue;
                }
                defaultClientDirectory = clientHome;
                break;
            }
            appDataPathList.clear();
            if (defaultClientDirectory == null) {
                String paths = String.join("\n - ", modifiedPathList);
                throw new InvalidUserDataException("Cannot discover client installation. Searched at: " + paths);
            }
            return defaultClientDirectory.getAbsolutePath();
        });
    }

    private static @NotNull List<String> getAppDataPathList() {
        List<String> appDataDirectories = new ArrayList<>();

        String userHome = System.getProperty("user.home");
        String dataHomeEnv = System.getenv("XDG_DATA_HOME");
        String appDataEnv = System.getenv("APPDATA");

        if (appDataEnv != null && !appDataEnv.trim().isEmpty()) {
            // Windows
            appDataDirectories.add(appDataEnv);
        }
        if (userHome != null && !userHome.trim().isEmpty()) {
            // Windows
            appDataDirectories.add(userHome + "/AppData/Roaming/");
            // Mac
            appDataDirectories.add(userHome + "/Library/Application Support/");
            // Linux
            appDataDirectories.add(userHome + "/.var/app/com.hypixel.HytaleLauncher/data/");
        }
        if (dataHomeEnv != null && !dataHomeEnv.trim().isEmpty()) {
            // Linux
            appDataDirectories.add(dataHomeEnv + "/.local/share/");
        }

        return appDataDirectories;
    }
}
