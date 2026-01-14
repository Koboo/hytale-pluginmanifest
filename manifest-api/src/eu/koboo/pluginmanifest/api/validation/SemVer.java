package eu.koboo.pluginmanifest.api.validation;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;

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
    public static SemVer parseString(List<ValidationResult> resultList, String key, String versionString) {
        String build = null;
        if(versionString.contains("+")) {
            String[] buildSplit = versionString.split("\\+");
            build = buildSplit[1];
            versionString = buildSplit[0];
        }

        String[] releases = null;
        if(versionString.contains("-")) {
            String[] releaseSplit = versionString.split("-");
            versionString = releaseSplit[0];
            releases = releaseSplit[1].split("\\.");
        }

        String[] split = versionString.split("\\.");
        if (split.length != 3) {
            resultList.add(ValidationResult.of(key, versionString, "must have 3 number segments"));
            return null;
        }

        int major = parseVersionNumber(resultList, key, "major", split[0]);
        int minor = parseVersionNumber(resultList, key, "minor", split[1]);
        int patch = parseVersionNumber(resultList, key, "patch", split[2]);
        if(major == Integer.MIN_VALUE || minor == Integer.MIN_VALUE || patch == Integer.MIN_VALUE) {
            return null;
        }

        return new SemVer(major, minor, patch, releases, build);
    }

    private static int parseVersionNumber(List<ValidationResult> resultList, String key, String type, String segmentString) {
        try {
            int versionNumber = Integer.parseInt(segmentString);
            if(versionNumber < 0) {
                resultList.add(ValidationResult.of(key, type, "version segment \"" + type + "\" must be positive (>= 0)"));
                return Integer.MIN_VALUE;
            }
            return versionNumber;
        } catch (NumberFormatException e) {
            resultList.add(ValidationResult.of(key, segmentString, "version segment \"" + type + "\" must be a number"));
            return Integer.MIN_VALUE;
        }
    }
}
