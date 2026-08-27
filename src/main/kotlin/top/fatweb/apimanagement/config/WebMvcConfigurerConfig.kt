package top.fatweb.apimanagement.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import top.fatweb.apimanagement.annotation.ApiController

/**
 * Web MVC configurer configuration
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see WebMvcConfigurer
 */
@Configuration
class WebMvcConfigurerConfig : WebMvcConfigurer {
    override fun configurePathMatch(configurer: PathMatchConfigurer) {
        configurer.addPathPrefix("/api/{PLUGIN}/v{API_VERSION}") { it.isAnnotationPresent(ApiController::class.java) }
    }
}
