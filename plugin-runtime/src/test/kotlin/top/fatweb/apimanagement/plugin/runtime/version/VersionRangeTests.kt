package top.fatweb.apimanagement.plugin.runtime.version

import top.fatweb.apimanagement.api.v1.version.VersionRange
import top.fatweb.apimanagement.api.v1.version.VersionRangeException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
class VersionRangeTests {
    @Test
    fun `supports caret ranges`() {
        val range = VersionRange.parse("^1.2.3")
        assertTrue(range.contains("1.2.3"))
        assertTrue(range.contains("1.9.0"))
        assertFalse(range.contains("2.0.0"))
    }

    @Test
    fun `supports caret ranges below one`() {
        val range = VersionRange.parse("^0.2.3")
        assertTrue(range.contains("0.2.9"))
        assertFalse(range.contains("0.3.0"))
    }

    @Test
    fun `supports tilde ranges`() {
        val range = VersionRange.parse("~1.2.3")
        assertTrue(range.contains("1.2.9"))
        assertFalse(range.contains("1.3.0"))
    }

    @Test
    fun `supports comparator clauses`() {
        val range = VersionRange.parse(">=1.1.0 <2.0.0")
        assertTrue(range.contains("1.1.0"))
        assertTrue(range.contains("1.9.9"))
        assertFalse(range.contains("2.0.0"))
        assertFalse(range.contains("1.0.9"))
    }

    @Test
    fun `supports alternatives`() {
        val range = VersionRange.parse("^1.0.0 || ^2.0.0")
        assertTrue(range.contains("1.5.0"))
        assertTrue(range.contains("2.1.0"))
        assertFalse(range.contains("3.0.0"))
    }

    @Test
    fun `supports wildcard`() {
        assertTrue(VersionRange.parse("*").contains("99.0.0"))
    }

    @Test
    fun `a release is newer than its pre-release`() {
        assertTrue(VersionRange.parse(">=1.0.0").contains("1.0.0"))
        assertFalse(VersionRange.parse(">=1.0.0").contains("1.0.0-SNAPSHOT"))
    }

    @Test
    fun `derives the minimum version and major`() {
        val range = VersionRange.parse(">=1.2.0 <2.0.0")
        assertEquals("1.2.0", range.minimumVersion().toString())
        assertEquals(1, range.minimumMajor())
    }

    @Test
    fun `rejects unbounded or multi-major ranges`() {
        assertFailsWith<VersionRangeException> { VersionRange.parse("*").minimumMajor() }
        assertFailsWith<VersionRangeException> {
            VersionRange.parse("^1.0 || ^2.0").minimumMajor()
        }
    }
}
