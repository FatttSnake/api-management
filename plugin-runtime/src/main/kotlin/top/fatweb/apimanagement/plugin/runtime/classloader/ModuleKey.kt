package top.fatweb.apimanagement.plugin.runtime.classloader

/**
 * Identity of a shared module class loader.
 *
 * @property coordinate Maven coordinate `group:name:version`
 * @property sha256 module jar digest
 * @property dependencies fingerprints of direct dependency keys, sorted
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
data class ModuleKey(
    val coordinate: String,
    val sha256: String,
    val dependencies: List<String> = emptyList()
) {
    /**
     * Stable fingerprint used in dependency lists.
     *
     * @return fingerprint
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun fingerprint(): String = buildString {
        append(coordinate)
        append('@')
        append(sha256)
        if (dependencies.isNotEmpty()) {
            append('[')
            append(dependencies.joinToString(","))
            append(']')
        }
    }
}
