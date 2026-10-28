package top.fatweb.apimanagement.plugin.runtime

import java.io.ByteArrayOutputStream
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import kotlin.reflect.KClass

/**
 * Builds a jar from already compiled test fixture classes.
 *
 * @param classes fixture classes to include
 * @param marker optional marker resource, used to give two otherwise identical jars different
 *               digests
 * @return jar bytes
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
fun jarOf(
    vararg classes: KClass<*>,
    marker: String? = null,
    entries: Map<String, ByteArray> = emptyMap()
): ByteArray {
    val output = ByteArrayOutputStream()
    JarOutputStream(output).use { jar ->
        classes.forEach { type ->
            val resource = type.java.name.replace('.', '/') + ".class"
            val bytes = type.java.classLoader.getResourceAsStream(resource)?.readBytes()
                ?: error("Missing class resource $resource")
            jar.putNextEntry(JarEntry(resource))
            jar.write(bytes)
            jar.closeEntry()
        }
        entries.forEach { (name, bytes) ->
            jar.putNextEntry(JarEntry(name))
            jar.write(bytes)
            jar.closeEntry()
        }
        if (marker != null) {
            jar.putNextEntry(JarEntry("fixture-marker.txt"))
            jar.write(marker.toByteArray())
            jar.closeEntry()
        }
    }
    return output.toByteArray()
}
