package top.fatweb.apimanagement.plugin.runtime.module

/**
 * One resolved dependency in a plugin lock.
 *
 * @property group Maven group
 * @property name Maven artifact name
 * @property version resolved version
 * @property sha256 module jar digest
 * @property size module jar size in bytes
 * @property source repository the module was resolved from
 * @property dependencies direct dependency digests
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
data class LockedModule(
    val group: String,
    val name: String,
    val version: String,
    val sha256: String,
    val size: Long = 0,
    val source: String? = null,
    val dependencies: List<String> = emptyList(),
) {
    /**
     * Maven coordinate.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val coordinate: String
        get() = "$group:$name:$version"
}

/**
 * Dependency lock carried by a plugin jar.
 *
 * @property format lock format version
 * @property constraints declared version ranges, keyed by `group:name`
 * @property modules resolved modules
 * @property groups modules that share one class loader, each group a list of module digests
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
data class PluginModuleLock(
    val format: Int = 1,
    val constraints: Map<String, String> = emptyMap(),
    val modules: List<LockedModule> = emptyList(),
    val groups: List<List<String>> = emptyList(),
) {
    /**
     * Validates the lock.
     *
     * @param maxModules maximum number of modules
     * @throws PluginModuleLockException when the lock is invalid
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun validate(maxModules: Int = DEFAULT_MAX_MODULES) {
        if (format != FORMAT) {
            throw PluginModuleLockException("Unsupported dependency lock format: $format")
        }
        if (modules.size > maxModules) {
            throw PluginModuleLockException("Dependency lock has ${modules.size} modules, limit is $maxModules")
        }

        val coordinates = mutableSetOf<String>()
        val digests = mutableSetOf<String>()
        modules.forEach { module ->
            requireCoordinate(module.group, "group")
            requireCoordinate(module.name, "name")
            requireCoordinate(module.version, "version")
            if (module.version.endsWith("-SNAPSHOT") ||
                module.version.equals("SNAPSHOT", ignoreCase = true) ||
                module.version.equals("LATEST", ignoreCase = true) ||
                module.version.equals("RELEASE", ignoreCase = true)
            ) {
                throw PluginModuleLockException("Dynamic version is not allowed: ${module.coordinate}")
            }
            if (!DIGEST.matches(module.sha256)) {
                throw PluginModuleLockException("Invalid SHA-256 for ${module.coordinate}: ${module.sha256}")
            }
            if (module.size < 0) {
                throw PluginModuleLockException("Invalid size for ${module.coordinate}: ${module.size}")
            }
            if (!coordinates.add(module.coordinate)) {
                throw PluginModuleLockException("Duplicate module coordinate: ${module.coordinate}")
            }
            if (!digests.add(module.sha256)) {
                throw PluginModuleLockException("Duplicate module digest: ${module.sha256}")
            }
        }

        val byDigest = modules.associateBy { it.sha256 }
        modules.forEach { module ->
            module.dependencies.forEach { dependency ->
                if (dependency == module.sha256) {
                    throw PluginModuleLockException("${module.coordinate} depends on itself")
                }
                if (dependency !in byDigest) {
                    throw PluginModuleLockException(
                        "${module.coordinate} depends on missing module digest $dependency",
                    )
                }
            }
        }
        requireAcyclic(modules, byDigest)
        validateGroups(byDigest)
        requireAcyclicUnits()

        constraints.forEach { (coordinate, range) ->
            if (coordinate.isBlank() || !coordinate.contains(':')) {
                throw PluginModuleLockException("Invalid dependency constraint key: $coordinate")
            }
            if (range.isBlank()) {
                throw PluginModuleLockException("Empty dependency constraint for $coordinate")
            }
        }
    }

    private fun validateGroups(byDigest: Map<String, LockedModule>) {
        val membership = HashMap<String, Int>()
        groups.forEachIndexed { index, group ->
            val label = "Shared loader group ${index + 1}"
            if (group.size < 2) {
                throw PluginModuleLockException("$label must contain at least two modules")
            }
            val seen = HashSet<String>()
            group.forEach { digest ->
                if (!DIGEST.matches(digest)) {
                    throw PluginModuleLockException("$label contains an invalid SHA-256: $digest")
                }
                if (!seen.add(digest)) {
                    throw PluginModuleLockException("$label lists module digest $digest more than once")
                }
                if (digest !in byDigest) {
                    throw PluginModuleLockException("$label references missing module digest $digest")
                }
                val previous = membership.putIfAbsent(digest, index)
                if (previous != null) {
                    throw PluginModuleLockException(
                        "Module digest $digest belongs to shared loader groups ${previous + 1} and ${index + 1}",
                    )
                }
            }
        }
    }

    private fun requireAcyclicUnits() {
        // Grouping can make the loader-unit graph cyclic even when module edges are acyclic
        loaderUnits()
    }

    private fun requireAcyclic(modules: List<LockedModule>, byDigest: Map<String, LockedModule>) {
        val visiting = mutableSetOf<String>()
        val visited = mutableSetOf<String>()

        fun visit(module: LockedModule) {
            if (module.sha256 in visited) {
                return
            }
            if (!visiting.add(module.sha256)) {
                throw PluginModuleLockException("Dependency cycle at ${module.coordinate}")
            }
            module.dependencies.forEach { dependency ->
                visit(byDigest.getValue(dependency))
            }
            visiting.remove(module.sha256)
            visited.add(module.sha256)
        }

        modules.forEach(::visit)
    }

    private fun requireCoordinate(value: String, label: String) {
        if (value.isBlank() || value.any { it.isWhitespace() }) {
            throw PluginModuleLockException("Invalid module $label: '$value'")
        }
    }

    companion object {
        /**
         * Supported lock format.
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        const val FORMAT: Int = 1

        /**
         * Default module count limit.
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        const val DEFAULT_MAX_MODULES: Int = 4096

        private val DIGEST = Regex("^[0-9a-f]{64}$")
    }
}

/**
 * Invalid dependency lock.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
class PluginModuleLockException(message: String) : RuntimeException(message)
