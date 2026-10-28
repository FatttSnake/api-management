package top.fatweb.apimanagement.api.v1.version

/**
 * A simple semantic version range.
 *
 * Supports `*`, exact versions, comparators (`>=`, `>`, `<=`, `<`, `=`), `^`, `~`, space-separated
 * AND groups and `||` OR groups. This is deliberately the subset a plugin descriptor needs;
 * dependency versions are locked at build time and never resolved here.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
class VersionRange private constructor(
    private val alternatives: List<Clause>
) {
    /**
     * Checks whether a version satisfies the range.
     *
     * @param version version text
     * @return true when satisfied
     * @throws VersionRangeException when the version is invalid
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun contains(version: String): Boolean {
        val parsed = Semver.parse(version)
        return alternatives.any { clause -> clause.predicates.all { predicate -> predicate(parsed) } }
    }

    /**
     * Returns the lowest version the range can accept.
     *
     * @return lowest version
     * @throws VersionRangeException when the range has no lower bound
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun minimumVersion(): Semver {
        val minima = alternatives.mapNotNull { it.minimum }
        if (minima.isEmpty() || minima.size != alternatives.size) {
            throw VersionRangeException("Version range has no lower bound")
        }
        return minima.min()
    }

    /**
     * Returns the single major version the range targets.
     *
     * @return major version
     * @throws VersionRangeException when the range is unbounded or spans majors
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun minimumMajor(): Int {
        val majors = alternatives.map { clause ->
            clause.minimum?.major
                ?: throw VersionRangeException("Version range has no lower bound")
        }.distinct()
        if (majors.size != 1) {
            throw VersionRangeException("Version range must target a single major version")
        }
        return majors.single()
    }

    private data class Clause(
        val predicates: List<(Semver) -> Boolean>,
        val minimum: Semver?
    )

    companion object {
        /**
         * Parses a version range.
         *
         * @param expression range expression
         * @return parsed range
         * @throws VersionRangeException when the expression is invalid
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        fun parse(expression: String): VersionRange {
            val text = expression.trim()
            if (text.isEmpty() || text == "*") {
                return VersionRange(listOf(Clause(emptyList(), null)))
            }
            val alternatives = text.split("||").map { alternative ->
                val clauses = alternative.trim().split(Regex("\\s+"))
                    .filter { it.isNotEmpty() }
                    .map(::clauseOf)
                if (clauses.isEmpty()) {
                    throw VersionRangeException("Invalid version range: $expression")
                }
                Clause(
                    predicates = clauses.flatMap { it.predicates },
                    minimum = clauses.mapNotNull { it.minimum }.maxOrNull()
                )
            }
            return VersionRange(alternatives)
        }

        private fun clauseOf(token: String): Clause {
            val operators = listOf(">=", "<=", ">", "<", "=", "^", "~")
            val operator = operators.firstOrNull { token.startsWith(it) }
            if (operator == null) {
                val exact = Semver.parse(token)
                return Clause(listOf { version -> version.compareTo(exact) == 0 }, exact)
            }
            val value = token.removePrefix(operator).trim()
            if (value.isEmpty()) {
                throw VersionRangeException("Invalid version range token: $token")
            }
            val (version, components) = Semver.parsePartial(value)
            return when (operator) {
                ">=" -> Clause(listOf { candidate -> candidate >= version }, version)
                ">" -> Clause(listOf { candidate -> candidate > version }, version)
                "<=" -> Clause(listOf { candidate -> candidate <= version }, null)
                "<" -> Clause(listOf { candidate -> candidate < version }, null)
                "=" -> Clause(listOf { candidate -> candidate.compareTo(version) == 0 }, version)
                "^" -> Clause(
                    listOf(
                        { candidate -> candidate >= version },
                        { candidate -> candidate < caretUpper(version, components) }
                    ),
                    version
                )
                "~" -> Clause(
                    listOf(
                        { candidate -> candidate >= version },
                        { candidate -> candidate < tildeUpper(version, components) }
                    ),
                    version
                )
                else -> throw VersionRangeException("Unknown operator: $operator")
            }
        }

        private fun caretUpper(version: Semver, components: Int): Semver = when {
            version.major > 0 -> Semver(version.major + 1, 0, 0)
            components >= 3 && version.minor == 0 -> Semver(0, 0, version.patch + 1)
            components >= 2 -> Semver(0, version.minor + 1, 0)
            else -> Semver(1, 0, 0)
        }

        private fun tildeUpper(version: Semver, components: Int): Semver = when {
            components >= 2 -> Semver(version.major, version.minor + 1, 0)
            else -> Semver(version.major + 1, 0, 0)
        }
    }
}
