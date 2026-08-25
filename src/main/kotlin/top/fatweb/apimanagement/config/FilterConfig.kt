package top.fatweb.apimanagement.config

import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import top.fatweb.apimanagement.filter.ExceptionFilter
import top.fatweb.apimanagement.filter.TraceIdFilter

/**
 * Filter configuration
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Configuration
class FilterConfig {
    @Bean
    fun exceptionFilterRegistrationBean(exceptionFilter: ExceptionFilter): FilterRegistrationBean<ExceptionFilter> =
        FilterRegistrationBean(exceptionFilter).apply {
            setBeanName("exceptionFilter")
            order = -100
        }

    @Bean
    fun traceIdFilterRegistrationBean(traceIdFilter: TraceIdFilter): FilterRegistrationBean<TraceIdFilter> =
        FilterRegistrationBean(traceIdFilter).apply {
            setBeanName("traceIdFilter")
            order = Ordered.HIGHEST_PRECEDENCE + 50
        }
}
