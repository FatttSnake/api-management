package top.fatweb.apimanagement.config

import org.springframework.boot.webmvc.autoconfigure.WebMvcRegistrations
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import top.fatweb.apimanagement.component.api.ApiResponseMappingHandlerMapping

/**
 * Web MVC registrations configuration
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see WebMvcRegistrations
 */
@Configuration
class WebMvcRegistrationsConfig : WebMvcRegistrations {
    override fun getRequestMappingHandlerMapping(): RequestMappingHandlerMapping = ApiResponseMappingHandlerMapping()
}
