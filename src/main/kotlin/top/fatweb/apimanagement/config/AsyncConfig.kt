package top.fatweb.apimanagement.config

import org.slf4j.MDC
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
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
     * @return ThreadPoolTaskExecutor
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ThreadPoolTaskExecutor
     */
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
