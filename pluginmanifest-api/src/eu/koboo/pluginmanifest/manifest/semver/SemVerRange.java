package eu.koboo.pluginmanifest.manifest.semver;

import eu.koboo.pluginmanifest.manifest.validation.ValidationResult;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SemVerRange {

    RangeType type;
    SemVer minVersion;
    SemVer maxVersion;
    boolean minInclusive;
    boolean maxInclusive;

    // Parses range strings like: >=1.2.3, ^1.2.3, ~1.2.3, 1.2.3 - 2.0.0, *, etc.
    public static SemVerRange parseString(List<ValidationResult> resultList, String key, String rangeString) {
        rangeString = rangeString.trim();

        // Check for wildcard
        if (rangeString.equals("*") || rangeString.equalsIgnoreCase("x")) {
            return new SemVerRange(RangeType.WILDCARD, null, null, true, true);
        }

        // Check for hyphen range (1.2.3 - 2.0.0)
        if (rangeString.contains(" - ")) {
            return parseHyphenRange(resultList, key, rangeString);
        }

        // Check for caret range (^1.2.3)
        if (rangeString.startsWith("^")) {
            String stripPrefix = rangeString.substring(1).trim();
            return parseCaretRange(resultList, key, stripPrefix);
        }

        // Check for tilde range (~1.2.3)
        if (rangeString.startsWith("~")) {
            String stripPrefix = rangeString.substring(1).trim();
            return parseTildeRange(resultList, key, stripPrefix);
        }

        // Check for comparison operators
        if (rangeString.startsWith(">=")) {
            String stripPrefix = rangeString.substring(2).trim();
            return parseComparisonRange(resultList, key, stripPrefix, RangeType.GREATER_EQUAL, true, false);
        }
        if (rangeString.startsWith(">")) {
            String stripPrefix = rangeString.substring(1).trim();
            return parseComparisonRange(resultList, key, stripPrefix, RangeType.GREATER, false, false);
        }
        if (rangeString.startsWith("<=")) {
            String stripPrefix = rangeString.substring(2).trim();
            return parseComparisonRange(resultList, key, stripPrefix, RangeType.LESS_EQUAL, false, true);
        }
        if (rangeString.startsWith("<")) {
            String stripPrefix = rangeString.substring(1).trim();
            return parseComparisonRange(resultList, key, stripPrefix, RangeType.LESS, false, false);
        }

        // No operator, treat as an exact version
        SemVer version = SemVer.parseString(resultList, key, rangeString);
        if (version == null) {
            return null;
        }
        return new SemVerRange(RangeType.EXACT, version, version, true, true);
    }

    // Parses hyphen ranges: 1.2.3 - 2.0.0 (inclusive on both ends)
    private static SemVerRange parseHyphenRange(List<ValidationResult> resultList, String key, String rangeString) {
        String[] parts = rangeString.split(" - ");
        if (parts.length != 2) {
            resultList.add(ValidationResult.of(key, rangeString, "hyphen range must have exactly two versions"));
            return null;
        }

        SemVer minVersion = SemVer.parseString(resultList, key + ".min", parts[0].trim());
        SemVer maxVersion = SemVer.parseString(resultList, key + ".max", parts[1].trim());

        if (minVersion == null || maxVersion == null) {
            return null;
        }

        return new SemVerRange(RangeType.HYPHEN, minVersion, maxVersion, true, true);
    }

    // Parses caret ranges: ^1.2.3 allows changes that don't modify left-most non-zero digit
    // ^1.2.3 := >=1.2.3 <2.0.0
    // ^0.2.3 := >=0.2.3 <0.3.0
    // ^0.0.3 := >=0.0.3 <0.0.4
    private static SemVerRange parseCaretRange(List<ValidationResult> resultList, String key, String versionString) {
        SemVer minVersion = SemVer.parseString(resultList, key, versionString);
        if (minVersion == null) {
            return null;
        }

        SemVer maxVersion;
        if (minVersion.getMajor() > 0) {
            // ^1.2.3 := >=1.2.3 <2.0.0
            maxVersion = new SemVer(minVersion.getMajor() + 1, 0, 0, null, null);
        } else if (minVersion.getMinor() > 0) {
            // ^0.2.3 := >=0.2.3 <0.3.0
            maxVersion = new SemVer(0, minVersion.getMinor() + 1, 0, null, null);
        } else {
            // ^0.0.3 := >=0.0.3 <0.0.4
            maxVersion = new SemVer(0, 0, minVersion.getPatch() + 1, null, null);
        }

        return new SemVerRange(RangeType.CARET, minVersion, maxVersion, true, false);
    }

    // Parses tilde ranges: ~1.2.3 allows patch-level changes
    // ~1.2.3 := >=1.2.3 <1.3.0
    // ~1.2 := >=1.2.0 <1.3.0
    // ~1 := >=1.0.0 <2.0.0
    private static SemVerRange parseTildeRange(List<ValidationResult> resultList, String key, String versionString) {
        SemVer minVersion = SemVer.parseString(resultList, key, versionString);
        if (minVersion == null) {
            return null;
        }

        // ~1.2.3 := >=1.2.3 <1.3.0
        SemVer maxVersion = new SemVer(minVersion.getMajor(), minVersion.getMinor() + 1, 0, null, null);

        return new SemVerRange(RangeType.TILDE, minVersion, maxVersion, true, false);
    }

    // Parses comparison ranges: >=1.2.3, >1.2.3, <=1.2.3, <1.2.3
    private static SemVerRange parseComparisonRange(List<ValidationResult> resultList, String key,
                                                    String versionString, RangeType type,
                                                    boolean minInclusive, boolean maxInclusive) {
        SemVer version = SemVer.parseString(resultList, key, versionString);
        if (version == null) {
            return null;
        }

        // For greater than operators, a version is the minimum
        // For less than operators, a version is the maximum
        boolean isGreater = type == RangeType.GREATER || type == RangeType.GREATER_EQUAL;
        SemVer minVersion = isGreater ? version : null;
        SemVer maxVersion = isGreater ? null : version;

        return new SemVerRange(type, minVersion, maxVersion, minInclusive, maxInclusive);
    }
}