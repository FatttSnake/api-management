package top.fatweb.apimanagement.api.v1

import java.time.Duration

/**
 * Scheduler owned by the gateway.
 *
 * All scheduled tasks are cancelled when the plugin stops or is uninstalled.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
interface Scheduler {
    /**
     * Schedules a task with a fixed delay.
     *
     * @param initialDelay delay before the first run
     * @param delay delay between runs
     * @param task task
     * @return cancelation handle
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun scheduleWithFixedDelay(initialDelay: Duration, delay: Duration, task: Runnable): Subscription

    /**
     * Schedules a task with a cron expression.
     *
     * @param expression cron expression
     * @param task task
     * @return cancelation handle
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun scheduleCron(expression: String, task: Runnable): Subscription
}
