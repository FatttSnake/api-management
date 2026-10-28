package top.fatweb.apimanagement.api.v1

/**
 * A cancelable registration, such as an event subscription or a scheduled task.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
fun interface Subscription : AutoCloseable {
    /**
     * Cancels the registration. Calling it more than once is a no-op.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    override fun close()
}
