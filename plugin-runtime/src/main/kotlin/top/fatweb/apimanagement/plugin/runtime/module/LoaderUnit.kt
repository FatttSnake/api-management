package top.fatweb.apimanagement.plugin.runtime.module

/**
 * Modules loaded by one class loader.
 *
 * A unit is either a declared shared-loader group or one ungrouped module.
 *
 * @property id unit ID, stable within one lock
 * @property members modules of the unit, sorted by digest
 * @property dependencies IDs of the units this one depends on, sorted
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
internal data class LoaderUnit(val id: Int, val members: List<LockedModule>, val dependencies: List<Int>)

/**
 * Builds the loader units of a validated lock.
 *
 * @return units in dependency-first order
 * @throws PluginModuleLockException when grouping creates a cycle between units
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
internal fun PluginModuleLock.loaderUnits(): List<LoaderUnit> {
    val byDigest = modules.associateBy { it.sha256 }
    val unitByDigest = HashMap<String, Int>()
    val members = ArrayList<List<LockedModule>>()

    groups.forEach { group ->
        val id = members.size
        val unitMembers = group.map { byDigest.getValue(it) }.sortedBy { it.sha256 }
        unitMembers.forEach { unitByDigest[it.sha256] = id }
        members.add(unitMembers)
    }
    modules.forEach { module ->
        if (module.sha256 !in unitByDigest) {
            unitByDigest[module.sha256] = members.size
            members.add(listOf(module))
        }
    }

    val dependencies =
        members.mapIndexed { id, unitMembers ->
            unitMembers
                .asSequence()
                .flatMap { it.dependencies.asSequence() }
                .map { unitByDigest.getValue(it) }
                .filter { it != id }
                .distinct()
                .sorted()
                .toList()
        }

    return orderUnits(members, dependencies)
}

private fun orderUnits(members: List<List<LockedModule>>, dependencies: List<List<Int>>): List<LoaderUnit> {
    val visiting = HashSet<Int>()
    val visited = HashSet<Int>()
    val ordered = ArrayList<LoaderUnit>()

    fun canonicalKey(id: Int): String = members[id].joinToString("|") { "${it.coordinate}@${it.sha256}" }

    fun visit(id: Int) {
        if (id in visited) {
            return
        }
        if (!visiting.add(id)) {
            throw PluginModuleLockException(
                "Shared loader groups create a dependency cycle between loader units: " +
                    members[id].joinToString { it.coordinate },
            )
        }
        val orderedDependencies = dependencies[id].sortedBy(::canonicalKey)
        orderedDependencies.forEach(::visit)
        visiting.remove(id)
        visited.add(id)
        ordered.add(LoaderUnit(id, members[id], orderedDependencies))
    }

    members.indices.sortedBy(::canonicalKey).forEach(::visit)
    return ordered
}
