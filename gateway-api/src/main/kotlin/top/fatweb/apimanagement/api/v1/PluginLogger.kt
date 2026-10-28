package top.fatweb.apimanagement.api.v1

/**
 * Plugin logger.
 *
 * Messages carry plugin and request context so one plugin's output can be separated from
 * another's.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
interface PluginLogger {
    /**
     * Logs at debug level.
     *
     * @param format SLF4J-style format string
     * @param args format arguments
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun debug(format: String, vararg args: Any?)

    /**
     * Logs at info level.
     *
     * @param format SLF4J-style format string
     * @param args format arguments
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun info(format: String, vararg args: Any?)

    /**
     * Logs at warn level.
     *
     * @param format SLF4J-style format string
     * @param args format arguments
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun warn(format: String, vararg args: Any?)

    /**
     * Logs at error level.
     *
     * @param format SLF4J-style format string
     * @param args format arguments
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun error(format: String, vararg args: Any?)

    /**
     * Logs at error level with a throwable.
     *
     * @param format SLF4J-style format string
     * @param cause throwable
     * @param args format arguments
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun error(format: String, cause: Throwable, vararg args: Any?)
}
