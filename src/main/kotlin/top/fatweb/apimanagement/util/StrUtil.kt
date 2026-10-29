package top.fatweb.apimanagement.util

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom

private val UPPER_CASE_LETTER = Regex("[A-Z]")

private val UNDERSCORE_LOWER_LETTER = Regex("_[a-z]")

private const val PASSWORD_CHARACTERS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"

private val SECURE_RANDOM = SecureRandom()

/**
 * Convert CamelCase string to underscore-delimited string
 *
 * @param str CamelCase string
 * @return Underscore-delimited string
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
fun upperToUnderLetter(str: String?): String {
    str ?: return ""

    return UPPER_CASE_LETTER.replace(str) { matchResult -> "_${matchResult.value.lowercase()}" }
}

/**
 * Convert underscore-delimited string to CamelCase string
 *
 * @param str Underscore-delimited string
 * @return CamelCase string
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
fun underToUpperLetter(str: String?): String {
    str ?: return ""

    return UNDERSCORE_LOWER_LETTER.replace(str) { matchResult ->
        matchResult.value.removePrefix("_").uppercase()
    }
}

/**
 * Generate random password
 *
 * Drawn from [SecureRandom], so the value is safe to use as a secret.
 *
 * @param length Length of password
 * @return Random password
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
fun generateRandomPassword(length: Int): String = buildString(length) {
    repeat(length) {
        append(PASSWORD_CHARACTERS[SECURE_RANDOM.nextInt(PASSWORD_CHARACTERS.length)])
    }
}

/**
 * Get MD5 of a string
 *
 * @param content Content
 * @return MD5 of content
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
fun md5(content: String): String = MessageDigest.getInstance("MD5")
    .digest(content.toByteArray(StandardCharsets.UTF_8))
    .toHexString()

/**
 * Get SHA-256 of a string
 *
 * @param content Content
 * @return SHA-256 hex of content
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
fun sha256(content: String): String = MessageDigest.getInstance("SHA-256")
    .digest(content.toByteArray(StandardCharsets.UTF_8))
    .toHexString()
