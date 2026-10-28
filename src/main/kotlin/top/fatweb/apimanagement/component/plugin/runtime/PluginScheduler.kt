package top.fatweb.apimanagement.component.plugin.runtime

import org.springframework.scheduling.support.CronExpression
import org.springframework.stereotype.Component
import top.fatweb.apimanagement.api.v1.Scheduler
import top.fatweb.apimanagement.api.v1.Subscription
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Scheduler for plugin tasks.
 *
 * All tasks are tracked per plugin so an uninstall can cancel every remaining timer.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Component
class PluginScheduler : AutoCloseable {
    private val executor: ScheduledExecutorService = Executors.newScheduledThreadPool(
        Runtime.getRuntime().availableProcessors().coerceIn(1, 4)
    ) { runnable ->
        Thread(runnable, "plugin-scheduler").apply { isDaemon = true }
    }
    private val tasks = ConcurrentHashMap<String, MutableSet<ScheduledFuture<*>>>()

    /**
     * Schedules a fixed-delay task.
     *
     * @param pluginId plugin ID
     * @param initialDelay initial delay
     * @param delay delay between runs
     * @param task task
     * @return cancellation handle
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun scheduleWithFixedDelay(
        pluginId: String,
        initialDelay: Duration,
        delay: Duration,
        task: Runnable
    ): Subscription {
        val future = executor.scheduleWithFixedDelay(
            { runCatching { task.run() } },
            initialDelay.toMillis().coerceAtLeast(0),
            delay.toMillis().coerceAtLeast(1),
            TimeUnit.MILLISECONDS
        )
        track(pluginId, future)
        return Subscription {
            future.cancel(false)
            untrack(pluginId, future)
        }
    }

    /**
     * Schedules a cron task.
     *
     * @param pluginId plugin ID
     * @param expression cron expression
     * @param task task
     * @return cancellation handle
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun scheduleCron(pluginId: String, expression: String, task: Runnable): Subscription {
        val cron = CronExpression.parse(expression)
        val cancelled = AtomicBoolean(false)
        val holder = arrayOfNulls<ScheduledFuture<*>>(1)

        fun scheduleNext() {
            if (cancelled.get()) {
                return
            }
            val now = LocalDateTime.now()
            val next = cron.next(now) ?: return
            val delay = Duration.between(now, next).toMillis().coerceAtLeast(0)
            val future = executor.schedule(
                {
                    if (!cancelled.get()) {
                        runCatching { task.run() }
                        scheduleNext()
                    }
                },
                delay,
                TimeUnit.MILLISECONDS
            )
            holder[0] = future
            track(pluginId, future)
        }

        scheduleNext()
        return Subscription {
            cancelled.set(true)
            holder[0]?.cancel(false)
            holder[0]?.let { untrack(pluginId, it) }
        }
    }

    /**
     * Drops every task owned by a plugin.
     *
     * @param pluginId plugin ID
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun removePlugin(pluginId: String) {
        tasks.remove(pluginId)?.forEach { it.cancel(false) }
    }

    /**
     * Returns a plugin-scoped facade.
     *
     * @param pluginId plugin ID
     * @return scheduler capability
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun scoped(pluginId: String): Scheduler = object : Scheduler {
        override fun scheduleWithFixedDelay(
            initialDelay: Duration,
            delay: Duration,
            task: Runnable
        ): Subscription = this@PluginScheduler.scheduleWithFixedDelay(pluginId, initialDelay, delay, task)

        override fun scheduleCron(expression: String, task: Runnable): Subscription =
            this@PluginScheduler.scheduleCron(pluginId, expression, task)
    }

    /**
     * Stops the scheduler.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    override fun close() {
        tasks.clear()
        executor.shutdownNow()
    }

    private fun track(pluginId: String, future: ScheduledFuture<*>) {
        tasks.computeIfAbsent(pluginId) { ConcurrentHashMap.newKeySet() }.add(future)
    }

    private fun untrack(pluginId: String, future: ScheduledFuture<*>) {
        tasks[pluginId]?.remove(future)
    }
}
