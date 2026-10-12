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
     * @return ThreadPoolTaskExecutor
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
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
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
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
