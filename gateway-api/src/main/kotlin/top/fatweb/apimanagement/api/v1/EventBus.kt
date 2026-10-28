package top.fatweb.apimanagement.api.v1

/**
 * In-process event bus.
 *
 * Subscriptions are cancelled automatically when the plugin stops or is uninstalled.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
interface EventBus {
    /**
     * Publishes an event.
     *
     * @param topic topic
     * @param payload raw payload
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun publish(topic: String, payload: ByteArray)

    /**
     * Subscribes to a topic.
     *
     * @param topic topic
     * @param subscriber subscriber
     * @return subscription handle
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun subscribe(topic: String, subscriber: EventSubscriber): Subscription

    /**
     * Receives one event.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun interface EventSubscriber {
        /**
         * Handles an event.
         *
         * @param topic topic
         * @param payload raw payload
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        fun onEvent(topic: String, payload: ByteArray)
    }
}
