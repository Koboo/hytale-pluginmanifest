package eu.koboo.pluginmanifest.manifest.semver;

public enum RangeType {
    // 1.2.3
    EXACT,
    // >1.2.3
    GREATER,
    // >=1.2.3
    GREATER_EQUAL,
    // <1.2.3
    LESS,
    // <=1.2.3
    LESS_EQUAL,
    // 1.2.3 - 2.0.0
    HYPHEN,
    // ^1.2.3 (compatible with)
    CARET,
    // ~1.2.3 (approximately equivalent to)
    TILDE,
    // * or x
    WILDCARD
}