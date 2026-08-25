package top.fatweb.apimanagement.config

import org.springframework.boot.jackson.autoconfigure.JacksonProperties
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.ext.javatime.ser.LocalDateTimeSerializer
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.module.SimpleModule
import tools.jackson.module.kotlin.KotlinModule
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Jackson configuration
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Configuration
class JacksonConfig {
    @Bean
    fun jsonMapperBuilderCustomizer(jacksonProperties: JacksonProperties): JsonMapperBuilderCustomizer =
        JsonMapperBuilderCustomizer { builder ->
            builder
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .addModules(KotlinModule.Builder().build())

            jacksonProperties.dateFormat?.let { dateFormat ->
                builder.addModules(
                    SimpleModule().addSerializer(
                        LocalDateTime::class.java,
                        LocalDateTimeSerializer(DateTimeFormatter.ofPattern(dateFormat))
                    )
                )
            }
        }

    @Bean
    fun jacksonConverterFactory(jsonMapper: JsonMapper): Jackson3ConverterFactory =
        Jackson3ConverterFactory.create(jsonMapper)
}
