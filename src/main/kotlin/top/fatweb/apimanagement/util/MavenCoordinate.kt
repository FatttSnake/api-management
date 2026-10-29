package top.fatweb.apimanagement.util

/**
 * Maven coordinate read from a module jar's Maven metadata.
 *
 * @property groupId Maven group
 * @property artifactId Maven artifact
 * @property version Maven version
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
data class MavenCoordinate(val groupId: String, val artifactId: String, val version: String) {
    override fun toString(): String = "$groupId:$artifactId:$version"
}
