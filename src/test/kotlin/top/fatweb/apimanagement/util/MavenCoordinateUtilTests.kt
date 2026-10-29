package top.fatweb.apimanagement.util

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Maven coordinate util tests
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see MavenCoordinateUtil
 */
class MavenCoordinateUtilTests {
    @Test
    fun `a jar declaring one coordinate is read`() {
        val coordinate =
            MavenCoordinateUtil.read(
                jar("META-INF/maven/org.example/example-core/pom.properties" to POM_PROPERTIES),
            )

        assertEquals(MavenCoordinate("org.example", "example-core", "1.2.3"), coordinate)
    }

    @Test
    fun `a jar without maven metadata has no coordinate`() {
        assertNull(MavenCoordinateUtil.read(jar("META-INF/MANIFEST.MF" to "Manifest-Version: 1.0\n")))
    }

    @Test
    fun `a jar declaring several coordinates has none`() {
        val coordinate =
            MavenCoordinateUtil.read(
                jar(
                    "META-INF/maven/org.example/first/pom.properties" to POM_PROPERTIES,
                    "META-INF/maven/org.example/second/pom.properties" to
                        "groupId=org.example\nartifactId=second\nversion=2.0.0\n",
                ),
            )

        assertNull(coordinate)
    }

    @Test
    fun `a jar missing a coordinate field has none`() {
        assertNull(
            MavenCoordinateUtil.read(
                jar("META-INF/maven/org.example/example-core/pom.properties" to "artifactId=example-core\n"),
            ),
        )
    }

    @Test
    fun `bytes that are not a jar have no coordinate`() {
        assertNull(MavenCoordinateUtil.read("not a jar".toByteArray()))
    }

    private fun jar(vararg entries: Pair<String, String>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            entries.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray())
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }

    private companion object {
        const val POM_PROPERTIES = "groupId=org.example\nartifactId=example-core\nversion=1.2.3\n"
    }
}
