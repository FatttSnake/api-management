package top.fatweb.apimanagement.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import top.fatweb.apimanagement.properties.ServerProperties

/**
 * Swagger API doc configuration
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ServerProperties
 */
@Configuration
class SwaggerConfig(
    private val buildProperties: BuildProperties
) {
    @Bean
    fun customOpenAPI(): OpenAPI {
        val contact = Contact().name("FatttSnake").url("https://fatweb.top").email("fatttsnake@gmail.com")
        return OpenAPI().info(
            Info().title("API Management 文档").description("API Management 后端 API 文档，包含各个 Controller 调用信息")
                .contact(contact).version(
                    buildProperties.version
                )
        )
    }
}
