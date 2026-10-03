package top.fatweb.apimanagement.config

import org.slf4j.MDC
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.task.TaskDecorator
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

/**
 * Async thread pool configuration
 *
 * Configures the [applicationTaskExecutor] with MDC context propagation
 * so that Trace ID (and other MDC values) are carried over to async threads.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Configuration
class AsyncConfig {
    /**
     * Application task executor with MDC context propagation
     *
     * Primary because it is the general-purpose pool; [pluginRemountExecutor] is a
     * single-purpose one, and an unqualified `Executor` injection (Spring MVC's async
     * support, for one) has to keep resolving to this.
     *
     * @return ThreadPoolTaskExecutor
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ThreadPoolTaskExecutor
     */
    @Primary
    @Bean("applicationTaskExecutor")
    fun applicationTaskExecutor(): ThreadPoolTaskExecutor =
        ThreadPoolTaskExecutor().apply {
            setTaskDecorator(MdcTaskDecorator())
            corePoolSize = 8
            maxPoolSize = Int.MAX_VALUE
            queueCapacity = Int.MAX_VALUE
            keepAliveSeconds = 60
            initialize()
        }

    /**
     * Executor that carries out post-commit plugin remounts
     *
     * Single-threaded on purpose: a remount rebuilds a child Spring context, opens datasource
     * pools and runs the plugin's lifecycle hooks, and a second remount beside it would only
     * wait on the same mount lock. One thread also gives remounts a thread nothing else uses,
     * so no datasource routing or security context leaks into or out of them.
     *
     * Deliberately not [applicationTaskExecutor]: that pool persists access and system logs,
     * and a remount waiting on an unreachable database would park one of its threads.
     *
     * The queue is unbounded, as [applicationTaskExecutor]'s is: a rejected submit would
     * surface inside a transaction's afterCommit, where there is no way to report it. Saves of
     * one plugin are coalesced before they reach the queue, so it stays short.
     *
     * @return ThreadPoolTaskExecutor
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ThreadPoolTaskExecutor
     */
    @Bean("pluginRemountExecutor")
    fun pluginRemountExecutor(): ThreadPoolTaskExecutor =
        ThreadPoolTaskExecutor().apply {
            setTaskDecorator(MdcTaskDecorator())
            corePoolSize = 1
            maxPoolSize = 1
            queueCapacity = Int.MAX_VALUE
            keepAliveSeconds = 60
            setThreadNamePrefix("plugin-remount-")
            initialize()
        }

    /**
     * MDC context propagation task decorator
     *
     * Copies MDC context from the submitting thread to the executing thread,
     * and restores the original context after execution.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see TaskDecorator
     */
    class MdcTaskDecorator : TaskDecorator {
        override fun decorate(task: Runnable): Runnable {
            val contextMap = MDC.getCopyOfContextMap()
            return Runnable {
                val previous = MDC.getCopyOfContextMap()
                try {
                    if (contextMap != null) {
                        MDC.setContextMap(contextMap)
                    } else {
                        MDC.clear()
                    }
                    task.run()
                } finally {
                    if (previous != null) {
                        MDC.setContextMap(previous)
                    } else {
                        MDC.clear()
                    }
                }
            }
        }
    }
}
