package top.fatweb.apimanagement.settings

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * Type of mail security
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
enum class MailSecurityType(val code: String) {
    NONE("None"),
    SSL_TLS("SSL/TLS"),
    START_TLS("StartTls");

    @JsonCreator
    fun fromCode(code: String): MailSecurityType? {
        entries.forEach {
            if (it.code == code) {
                return it
            }
        }
        return null
    }

    @JsonValue
    override fun toString(): String {
        return code
    }
}
