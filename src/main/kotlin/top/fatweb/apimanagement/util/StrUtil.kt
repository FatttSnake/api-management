package top.fatweb.apimanagement.util

import java.security.MessageDigest

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

    return Regex("[A-Z]").replace(str) { matchResult -> "_${matchResult.value.lowercase()}" }
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

    return Regex("_[a-z]").replace(str) { matchResult -> matchResult.value.removePrefix("_").uppercase() }
}

/**
 * Generate random password
 *
 * @param length Length of password
 * @return Random password
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
fun generateRandomPassword(length: Int): String {
    val characterSet = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
    val password = StringBuilder()

    (0 until length).forEach { _ ->
        password.append(characterSet.random())
    }

    return password.toString()
}

/**
 * Get MD5 of a string
 *
 * @param content Content
 * @return MD5 of content
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
fun md5(content: String): String {
    val hash = MessageDigest.getInstance("MD5").digest(content.toByteArray())
    val hex = StringBuilder(hash.size * 2)
    for (b in hash) {
        var str = Integer.toHexString(b.toInt())
        if (b < 0x10) {
            str = "0$str"
        }
        hex.append(str.substring(str.length - 2))
    }
    return hex.toString()
}

/**
 * Get SHA-256 of a string
 *
 * @param content Content
 * @return SHA-256 hex of content
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
fun sha256(content: String): String {
    val hash = MessageDigest.getInstance("SHA-256").digest(content.toByteArray())
    val hex = StringBuilder(hash.size * 2)
    for (b in hash) {
        var str = Integer.toHexString(b.toInt())
        if (b < 0x10) {
            str = "0$str"
        }
        hex.append(str.substring(str.length - 2))
    }
    return hex.toString()
}
