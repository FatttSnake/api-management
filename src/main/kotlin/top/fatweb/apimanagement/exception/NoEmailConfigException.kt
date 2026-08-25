package top.fatweb.apimanagement.exception

/**
 * Email settings not configured exception
 *
 * @param configs Configs not config
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see RuntimeException
 */
class NoEmailConfigException(
    vararg configs: String
) : RuntimeException("Email settings not configured: ${configs.joinToString(", ")}")
