package eu.koboo.pluginmanifest.gradle.plugin.tasks.validation.semver;

import eu.koboo.pluginmanifest.gradle.plugin.tasks.validation.ValidationException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SemVer {

    int major;
    int minor;
    int patch;
    String[] release;
    String build;

    // MAJOR.MINOR.PATCH-RELEASE.RELEASE.RELEASE+BUILD
    public static SemVer parseString(String key, String versionString) throws ValidationException {
        versionString = versionString.trim();
        String build = null;
        if (versionString.contains("+")) {
            String[] buildSplit = versionString.split("\\+");
            build = buildSplit[1];
            versionString = buildSplit[0];
        }

        String[] releases = null;
        if (versionString.contains("-")) {
            String[] releaseSplit = versionString.split("-");
            versionString = releaseSplit[0];
            String releaseString = releaseSplit[1];
            if(releaseString.contains(".")) {
                releases = releaseString.split("\\.");
            } else {
                releases = new String[]{releaseString};
            }
        }

        String[] split = versionString.split("\\.");
        if (split.length != 3) {
            throw new ValidationException(key, versionString, "must have 3 number segments");
        }

        int major = parseVersionNumber(key, versionString, "major", split[0]);
        int minor = parseVersionNumber(key, versionString, "minor", split[1]);
        int patch = parseVersionNumber(key, versionString, "patch", split[2]);
        if (major == Integer.MIN_VALUE || minor == Integer.MIN_VALUE || patch == Integer.MIN_VALUE) {
            return null;
        }

        return new SemVer(major, minor, patch, releases, build);
    }

    private static int parseVersionNumber(String key, String versionString, String type, String segmentString) throws ValidationException {
        try {
            int versionNumber = Integer.parseInt(segmentString);
            if (versionNumber < 0) {
                throw new ValidationException(key, versionString, "version segment \"" + type + "\" must be positive (>= 0)");
            }
            return versionNumber;
        } catch (NumberFormatException e) {
            throw new ValidationException(key, versionString, "version segment \"" + type + "\" must be a number");
        }
    }
}
