package top.fatweb.apimanagement.config

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.PropertyAccessor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
import tools.jackson.databind.DefaultTyping
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator

/**
 * Redis configuration
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Configuration
class RedisConfig {
    @Bean
    fun redisTemplate(
        redisConnectionFactory: RedisConnectionFactory,
        jsonMapper: JsonMapper
    ): RedisTemplate<*, *> {
        val redisTemplate = RedisTemplate<String, Any>()
        redisTemplate.connectionFactory = redisConnectionFactory
        val stringRedisSerializer = StringRedisSerializer()

        // Rebuild the auto-configured mapper with default typing enabled, so non-final
        // types carry a `@class` property and values read back as `Any` deserialize to
        // their original type.
        val redisObjectMapper = jsonMapper.rebuild()
            .changeDefaultVisibility { it.withVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY) }
            .activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                    .allowIfSubType("top.fatweb.apimanagement")
                    .allowIfSubType("java.util")
                    .allowIfSubType("java.time")
                    .build(),
                DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
            )
            .build()

        val anyJacksonJsonRedisSerializer = JacksonJsonRedisSerializer(redisObjectMapper, Any::class.java)

        // Use String Redis Serializer to serialize and deserialize redis key values
        redisTemplate.keySerializer = stringRedisSerializer
        redisTemplate.valueSerializer = anyJacksonJsonRedisSerializer

        // The Hash key also uses the String Redis Serializer serialization method.
        redisTemplate.hashKeySerializer = stringRedisSerializer
        redisTemplate.hashValueSerializer = anyJacksonJsonRedisSerializer

        redisTemplate.afterPropertiesSet()

        return redisTemplate
    }
}
