package top.fatweb.apimanagement.sdk.plugin

import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue

/**
 * Dependency index of a plugin jar
 *
 * Stored at [INDEX_ENTRY] inside the plugin jar. Each library is a nested jar at
 * [entryPath] that the gateway extracts to a content-addressed cache.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
data class PluginLibraryIndex(
    /**
     * Index format version
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val format: Int = FORMAT,

    /**
     * Bundled libraries
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val libraries: List<PluginLibraryEntry> = emptyList()
) {
    companion object {
        /**
         * Current index format
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        const val FORMAT = 1

        /**
         * Directory holding the nested library jars and the index
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        const val LIBRARY_DIR = "META-INF/api-plugin-libs/"

        /**
         * Jar entry path of the index
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        const val INDEX_ENTRY = LIBRARY_DIR + "index.json"

        /**
         * Pattern every library digest must match
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        val SHA256_REGEX = Regex("^[0-9a-f]{64}$")

        /**
         * Jar entry path of a nested library
         *
         * @param sha256 Library digest
         * @return Jar entry path
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        fun entryPath(sha256: String): String = "$LIBRARY_DIR$sha256.jar"

        /**
         * Parse an index
         *
         * @param json Index JSON
         * @return Parsed index
         * @throws IllegalArgumentException when the index is malformed
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        fun read(json: String): PluginLibraryIndex {
            val index = try {
                jacksonObjectMapper().readValue<PluginLibraryIndex>(json)
            } catch (e: Exception) {
                throw IllegalArgumentException("Invalid plugin library index: ${e.message}", e)
            }
            validate(index)
            return index
        }

        /**
         * Serialize an index
         *
         * @param index Index to serialize
         * @return Index JSON
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        fun write(index: PluginLibraryIndex): String {
            validate(index)
            return jacksonObjectMapper().writeValueAsString(index)
        }

        /**
         * Validate an index
         *
         * @param index Index to validate
         * @throws IllegalArgumentException when the index is malformed
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        fun validate(index: PluginLibraryIndex) {
            require(index.format == FORMAT) { "Unsupported plugin library index format: ${index.format}" }
            val digests = index.libraries.map { it.sha256 }.toSet()
            require(digests.size == index.libraries.size) { "Duplicate library digest in plugin library index" }
            val byDigest = index.libraries.associateBy { it.sha256 }
            index.libraries.forEach { library ->
                require(SHA256_REGEX.matches(library.sha256)) {
                    "Invalid library digest: '${library.sha256}'"
                }
                require(library.file.isNotBlank()) { "Library ${library.sha256} has no file name" }
                require('/' !in library.file && '\\' !in library.file) {
                    "Library ${library.sha256} has an invalid file name: '${library.file}'"
                }
                library.dependsOn.forEach { dependency ->
                    require(dependency != library.sha256) {
                        "Library ${library.sha256} depends on itself"
                    }
                    require(dependency in byDigest) {
                        "Library ${library.sha256} depends on missing library $dependency"
                    }
                }
            }
            require(isAcyclic(index)) { "Plugin library index contains a dependency cycle" }
        }

        /**
         * Whether the index bundles BouncyCastle
         *
         * @param index Index to inspect
         * @return True when a BouncyCastle module is bundled
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        fun bundlesBouncyCastle(index: PluginLibraryIndex): Boolean =
            index.libraries.any { library ->
                library.modules.any { it.startsWith("${PluginClasspath.BOUNCY_CASTLE_GROUP}:") }
            }

        private fun isAcyclic(index: PluginLibraryIndex): Boolean {
            val byDigest = index.libraries.associateBy { it.sha256 }
            val visiting = mutableSetOf<String>()
            val visited = mutableSetOf<String>()
            fun visit(digest: String): Boolean {
                if (digest in visited) return true
                if (!visiting.add(digest)) return false
                val library = byDigest[digest] ?: return false
                if (library.dependsOn.any { !visit(it) }) return false
                visiting.remove(digest)
                visited.add(digest)
                return true
            }
            return index.libraries.all { visit(it.sha256) }
        }
    }
}

/**
 * One bundled library
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
data class PluginLibraryEntry(
    /**
     * SHA-256 of the nested jar
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val sha256: String,

    /**
     * Original file name, for diagnostics only
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val file: String,

    /**
     * Modules contained in the jar
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val modules: List<String> = emptyList(),

    /**
     * Direct dependency digests
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val dependsOn: List<String> = emptyList()
)
