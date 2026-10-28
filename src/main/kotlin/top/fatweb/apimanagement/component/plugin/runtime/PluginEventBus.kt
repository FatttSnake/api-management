package top.fatweb.apimanagement.component.plugin.runtime

import org.springframework.stereotype.Component
import top.fatweb.apimanagement.api.v1.EventBus
import top.fatweb.apimanagement.api.v1.Subscription
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * In-process event bus for plugins.
 *
 * Every subscription is owned by a plugin, so an uninstall can drop all of them at once.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Component
class PluginEventBus {
    private data class Entry(val pluginId: String, val subscriber: EventBus.EventSubscriber)

    private val topics = ConcurrentHashMap<String, CopyOnWriteArrayList<Entry>>()

    /**
     * Publishes an event to every subscriber.
     *
     * @param topic topic
     * @param payload payload
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun publish(topic: String, payload: ByteArray) {
        topics[topic]?.forEach { entry ->
            runCatching { entry.subscriber.onEvent(topic, payload) }
        }
    }

    /**
     * Subscribes on behalf of a plugin.
     *
     * @param pluginId plugin ID
     * @param topic topic
     * @param subscriber subscriber
     * @return cancellation handle
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun subscribe(pluginId: String, topic: String, subscriber: EventBus.EventSubscriber): Subscription {
        val entry = Entry(pluginId, subscriber)
        val list = topics.computeIfAbsent(topic) { CopyOnWriteArrayList() }
        list.add(entry)
        return Subscription {
            list.remove(entry)
            if (list.isEmpty()) {
                topics.remove(topic, list)
            }
        }
    }

    /**
     * Drops every subscription owned by a plugin.
     *
     * @param pluginId plugin ID
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun removePlugin(pluginId: String) {
        topics.values.forEach { list ->
            list.removeIf { entry -> entry.pluginId == pluginId }
        }
        topics.entries.removeIf { entry -> entry.value.isEmpty() }
    }

    /**
     * Returns a plugin-scoped facade.
     *
     * @param pluginId plugin ID
     * @return event capability
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun scoped(pluginId: String): EventBus = object : EventBus {
        override fun publish(topic: String, payload: ByteArray) {
            this@PluginEventBus.publish(topic, payload)
        }

        override fun subscribe(topic: String, subscriber: EventBus.EventSubscriber): Subscription =
            this@PluginEventBus.subscribe(pluginId, topic, subscriber)
    }
}
