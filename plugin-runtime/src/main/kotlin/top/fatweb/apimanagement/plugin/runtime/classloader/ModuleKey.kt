package top.fatweb.apimanagement.plugin.runtime.classloader

/**
 * One member of a shared loader unit.
 *
 * @property coordinate Maven coordinate `group:name:version`
 * @property sha256 module jar digest
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
data class ModuleMember(val coordinate: String, val sha256: String)

/**
 * Identity of a shared module class loader.
 *
 * A unit is one module or one declared shared-loader group. Equality covers the member set and
 * the fingerprints of the units it depends on, so two plugins share a loader only when both
 * match.
 *
 * @param members modules the loader owns
 * @param dependencies fingerprints of the dependency units, sorted
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
class ModuleKey(members: Collection<ModuleMember>, dependencies: Collection<String> = emptyList()) {
    /**
     * Members of the unit, sorted by digest.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val members: List<ModuleMember> = members.sortedBy { it.sha256 }

    /**
     * Fingerprints of the dependency units, sorted.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val dependencies: List<String> = dependencies.sorted()

    /**
     * Stable fingerprint used in dependency lists.
     *
     * @return fingerprint
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun fingerprint(): String = buildString {
        append("unit[")
        append(members.joinToString(";") { "${it.coordinate}@${it.sha256}" })
        append(']')
        if (dependencies.isNotEmpty()) {
            append('[')
            append(dependencies.joinToString(","))
            append(']')
        }
    }

    /**
     * Compares unit identity.
     *
     * @param other other object
     * @return true when the member set and dependency fingerprints match
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean =
        other is ModuleKey && members == other.members && dependencies == other.dependencies

    /**
     * Hashes unit identity.
     *
     * @return hash code
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    override fun hashCode(): Int = 31 * members.hashCode() + dependencies.hashCode()

    /**
     * Returns the fingerprint.
     *
     * @return fingerprint
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    override fun toString(): String = fingerprint()
}
