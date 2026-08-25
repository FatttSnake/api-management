package top.fatweb.apimanagement.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import top.fatweb.apimanagement.aspectj.SysLogInterceptor

/**
 * System log configuration
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see SysLogInterceptor
 * @see WebMvcConfigurer
 */
@Configuration
class SysLogConfig(
    private val sysLogInterceptor: SysLogInterceptor
) : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(sysLogInterceptor).addPathPatterns("/**")
            .excludePathPatterns("/error/thrown", "/webjars/**")
    }
}
