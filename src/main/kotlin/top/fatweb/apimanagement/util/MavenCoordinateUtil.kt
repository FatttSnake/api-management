package top.fatweb.apimanagement.util

import java.util.Properties
import java.util.zip.ZipInputStream

/**
 * Reads the Maven coordinate a jar declares for itself.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see MavenCoordinate
 */
object MavenCoordinateUtil {
    private val POM_PROPERTIES = Regex("^META-INF/maven/[^/]+/[^/]+/pom\\.properties$")

    /**
     * Reads the coordinate from `META-INF/maven/<group>/<artifact>/pom.properties`.
     *
     * Not every jar carries that metadata - OSGi bundles such as BouncyCastle do not, and a
     * shaded jar can hold several coordinates - so an unreadable, missing or ambiguous result
     * is reported as null rather than guessed.
     *
     * @param bytes jar bytes
     * @return coordinate, or null when the jar does not declare exactly one
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun read(bytes: ByteArray): MavenCoordinate? {
        val coordinates = mutableSetOf<MavenCoordinate>()
        runCatching {
            ZipInputStream(bytes.inputStream()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (POM_PROPERTIES.matches(entry.name)) {
                        val properties =
                            Properties().apply {
                                load(zip)
                            }
                        val coordinate =
                            MavenCoordinate(
                                groupId = properties.getProperty("groupId")?.trim().orEmpty(),
                                artifactId = properties.getProperty("artifactId")?.trim().orEmpty(),
                                version = properties.getProperty("version")?.trim().orEmpty(),
                            )
                        if (
                            coordinate.groupId.isNotEmpty() &&
                            coordinate.artifactId.isNotEmpty() &&
                            coordinate.version.isNotEmpty()
                        ) {
                            coordinates.add(coordinate)
                        }
                    }
                    entry = zip.nextEntry
                }
            }
        }
        return coordinates.singleOrNull()
    }
}
