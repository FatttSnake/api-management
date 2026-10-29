package top.fatweb.apimanagement.api.v1.version

/**
 * A semantic version.
 *
 * @property major major version
 * @property minor minor version
 * @property patch patch version
 * @property qualifier pre-release qualifier, or null for a release
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
data class Semver(val major: Int, val minor: Int, val patch: Int, val qualifier: String? = null) : Comparable<Semver> {
    /**
     * Compares two versions, with a release above its pre-releases.
     *
     * @param other other version
     * @return comparison result
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    override fun compareTo(other: Semver): Int {
        val core = compareValuesBy(this, other, Semver::major, Semver::minor, Semver::patch)
        if (core != 0) {
            return core
        }
        return when {
            qualifier == null && other.qualifier == null -> 0
            qualifier == null -> 1
            other.qualifier == null -> -1
            else -> qualifier.compareTo(other.qualifier)
        }
    }

    override fun toString(): String = "$major.$minor.$patch" + (qualifier?.let { "-$it" } ?: "")

    companion object {
        /**
         * Parses a version such as `1.2.3`, `v1.2`, or `1.2.3-SNAPSHOT`.
         *
         * @param value version text
         * @return parsed version
         * @throws VersionRangeException when the text is not a version
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        fun parse(value: String): Semver {
            val normalized = value.trim().removePrefix("v").removePrefix("V")
            if (normalized.isEmpty()) {
                throw VersionRangeException("Empty version")
            }
            val withoutBuild = normalized.substringBefore('+')
            val core = withoutBuild.substringBefore('-')
            val qualifier = withoutBuild.substringAfter('-', "").ifEmpty { null }
            val parts = core.split('.')
            if (parts.size > 3 || parts.any { it.isEmpty() }) {
                throw VersionRangeException("Invalid version: $value")
            }
            val numbers =
                parts.map { part ->
                    part.toIntOrNull() ?: throw VersionRangeException("Invalid version: $value")
                }
            return Semver(
                major = numbers.getOrElse(0) { 0 },
                minor = numbers.getOrElse(1) { 0 },
                patch = numbers.getOrElse(2) { 0 },
                qualifier = qualifier,
            )
        }

        /**
         * Parses a partial version, returning the component count as well.
         *
         * @param value version text
         * @return parsed version and component count
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        fun parsePartial(value: String): Pair<Semver, Int> {
            val normalized = value.trim().removePrefix("v").removePrefix("V")
            val core = normalized.substringBefore('+').substringBefore('-')
            val count = core.split('.').size
            return parse(value) to count
        }
    }
}

/**
 * Invalid version or version range.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
class VersionRangeException(message: String) : RuntimeException(message)
