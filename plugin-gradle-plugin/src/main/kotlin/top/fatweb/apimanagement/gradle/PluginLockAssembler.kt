package top.fatweb.apimanagement.gradle

import tools.jackson.module.kotlin.jacksonObjectMapper

/**
 * One module of a plugin lock.
 *
 * @property group Maven group
 * @property name Maven artifact name
 * @property version resolved version
 * @property sha256 module jar digest
 * @property size module jar size
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
internal data class LockArtifact(
    val group: String,
    val name: String,
    val version: String,
    val sha256: String,
    val size: Long,
) {
    /**
     * Maven coordinate.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val coordinate: String
        get() = "$group:$name:$version"

    /**
     * Maven group and artifact.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val ga: String
        get() = "$group:$name"
}

/**
 * One module entry of the generated lock.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
internal data class LockModuleDocument(
    val group: String,
    val name: String,
    val version: String,
    val sha256: String,
    val size: Long,
    val dependencies: List<String>,
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
 * Contents of `META-INF/api-plugin-deps.json`.
 *
 * @property format lock format version
 * @property constraints resolved versions
 * @property modules modules, sorted by coordinate
 * @property groups shared-loader groups, each a sorted list of digests
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
internal data class LockDocument(
    val format: Int = 1,
    val constraints: Map<String, String>,
    val modules: List<LockModuleDocument>,
    val groups: List<List<String>>,
)

/**
 * Builds the plugin lock from resolved artifacts.
 *
 * The input is Resolved modules and their direct edges; the output is deterministic, so equal
 * inputs produce byte-identical JSON regardless of declaration order.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
internal object PluginLockAssembler {
    private val JSON = jacksonObjectMapper()

    /**
     * Assembles a lock.
     *
     * @param artifacts resolved modules, already filtered and keyed by coordinate
     * @param edges direct dependency edges, `from=to` with Maven coordinates
     * @param sharedLoaders groups of `group:artifact` values that must share one loader
     * @param gatewayProvided artifacts provided by the gateway and therefore not locked
     * @return lock document
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun assemble(
        artifacts: Collection<LockArtifact>,
        edges: Collection<String>,
        sharedLoaders: Collection<List<String>>,
        gatewayProvided: Set<String> = emptySet(),
    ): LockDocument {
        val byCoordinate = artifacts.associateBy { it.coordinate }
        val groups = resolveGroups(sharedLoaders, artifacts.groupBy { it.ga }, gatewayProvided)
        val edgesByFrom = parseEdges(edges)
        val modules =
            byCoordinate.values
                .sortedBy { it.coordinate }
                .map { artifact ->
                    val dependencies =
                        edgesByFrom[artifact.coordinate]
                            .orEmpty()
                            .mapNotNull { dependency -> byCoordinate[dependency]?.sha256 }
                            .filter { it != artifact.sha256 }
                            .distinct()
                            .sorted()
                    LockModuleDocument(
                        group = artifact.group,
                        name = artifact.name,
                        version = artifact.version,
                        sha256 = artifact.sha256,
                        size = artifact.size,
                        dependencies = dependencies,
                    )
                }
        requireAcyclicUnits(modules, groups)

        val constraints = LinkedHashMap<String, String>()
        byCoordinate.keys.sorted().forEach { coordinate ->
            val parts = coordinate.split(':')
            constraints["${parts[0]}:${parts[1]}"] = parts[2]
        }

        return LockDocument(format = 1, constraints = constraints, modules = modules, groups = groups)
    }

    /**
     * Serializes a lock.
     *
     * @param document lock document
     * @return lock JSON
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun toJson(document: LockDocument): String = JSON.writeValueAsString(document)

    private fun resolveGroups(
        sharedLoaders: Collection<List<String>>,
        byGa: Map<String, List<LockArtifact>>,
        gatewayProvided: Set<String>,
    ): List<List<String>> {
        val ownerByGa = HashMap<String, Int>()
        val groups =
            sharedLoaders.mapIndexed { index, group ->
                val label = "sharedLoader group ${index + 1}"
                if (group.size < 2) {
                    throw IllegalArgumentException("$label must contain at least two modules")
                }

                val seen = HashSet<String>()
                group.forEach { raw ->
                    val ga = raw.trim()
                    if (ga.count { it == ':' } != 1 || ga.any { it.isWhitespace() }) {
                        throw IllegalArgumentException("$label expects group:artifact without a version: $raw")
                    }
                    if (!seen.add(ga)) {
                        throw IllegalArgumentException("$label lists $ga more than once")
                    }
                    if (ga in gatewayProvided) {
                        throw IllegalArgumentException("$label names a module provided by the gateway: $ga")
                    }
                    val previous = ownerByGa.putIfAbsent(ga, index)
                    if (previous != null) {
                        throw IllegalArgumentException(
                            "$ga appears in more than one sharedLoader group: ${previous + 1} and ${index + 1}",
                        )
                    }
                }

                group
                    .map { raw ->
                        val ga = raw.trim()
                        val matches = byGa[ga].orEmpty()
                        when {
                            matches.isEmpty() ->
                                throw IllegalArgumentException("$label names a module that is not locked: $ga")

                            matches.size > 1 ->
                                throw IllegalArgumentException(
                                    "$label names an ambiguous module: $ga " +
                                        "(matches ${matches.map { it.coordinate }.sorted()})",
                                )
                        }
                        matches.single().sha256
                    }.sorted()
            }

        return groups.sortedBy { it.joinToString(",") }
    }

    private fun parseEdges(edges: Collection<String>): Map<String, List<String>> {
        val result = mutableMapOf<String, MutableList<String>>()
        edges.forEach { edge ->
            val separator = edge.indexOf('=')
            if (separator <= 0) {
                return@forEach
            }
            val from = edge.substring(0, separator)
            val to = edge.substring(separator + 1)
            result.getOrPut(from) { mutableListOf() }.add(to)
        }
        return result
    }

    private fun requireAcyclicUnits(modules: List<LockModuleDocument>, groups: List<List<String>>) {
        val unitByDigest = HashMap<String, Int>()
        groups.forEachIndexed { index, group ->
            group.forEach { digest -> unitByDigest[digest] = index }
        }
        modules.forEach { module ->
            if (module.sha256 !in unitByDigest) {
                unitByDigest[module.sha256] = unitByDigest.size
            }
        }
        val dependencies = HashMap<Int, MutableSet<Int>>()
        modules.forEach { module ->
            val unit = unitByDigest.getValue(module.sha256)
            module.dependencies.forEach { dependency ->
                val target = unitByDigest.getValue(dependency)
                if (target != unit) {
                    dependencies.getOrPut(unit) { mutableSetOf() }.add(target)
                }
            }
        }

        val visiting = HashSet<Int>()
        val visited = HashSet<Int>()

        fun visit(unit: Int) {
            if (unit in visited) {
                return
            }
            if (!visiting.add(unit)) {
                throw IllegalArgumentException(
                    "sharedLoader groups create a dependency cycle between loader units: " +
                        modules.filter { unitByDigest.getValue(it.sha256) == unit }
                            .joinToString { it.coordinate },
                )
            }
            dependencies[unit].orEmpty().forEach(::visit)
            visiting.remove(unit)
            visited.add(unit)
        }

        unitByDigest.values.toSortedSet().forEach(::visit)
    }
}
