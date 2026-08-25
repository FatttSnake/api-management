package top.fatweb.apimanagement.component.storage

import org.springframework.data.redis.core.BoundSetOperations
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

/**
 * Redis provider
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see RedisTemplate
 */
@Suppress("UNCHECKED_CAST")
@Component
class RedisProvider(
    private val redisTemplate: RedisTemplate<String, Any>
) {
    /**
     * Set valid time
     *
     * @param key Cache key
     * @param timeout Timeout
     * @param timeUnit Unit of timeout
     * @return true=success; false=fail
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see TimeUnit
     */
    fun setExpire(key: String, timeout: Long, timeUnit: TimeUnit = TimeUnit.SECONDS): Boolean =
        redisTemplate.expire(key, timeout, timeUnit)

    /**
     * Get valid time
     *
     * @param key Cache key
     * @param timeUnit Unit of time
     * @return Valid time
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun getExpire(key: String, timeUnit: TimeUnit = TimeUnit.SECONDS): Long = redisTemplate.getExpire(key, timeUnit)

    /**
     * Determine whether key exists
     *
     * @param key Cache key
     * @return true=exist; false=not exist
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun hasKey(key: String): Boolean = redisTemplate.hasKey(key)

    /**
     * Get list of cached key
     *
     * @param pattern Pattern of key
     * @return List of key
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun keys(pattern: String): Set<String> = redisTemplate.keys(pattern)

    /**
     * Cache basic object
     *
     * @param key   Cache key
     * @param value Cache object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun setObject(key: String, value: Any) = redisTemplate.opsForValue().set(key, value)

    /**
     * Cache basic object
     *
     * @param key   Cache key
     * @param value Cache object
     * @param timeout Timeout
     * @param timeUnit Unit of timeout
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun setObject(key: String, value: Any, timeout: Long, timeUnit: TimeUnit = TimeUnit.SECONDS) =
        redisTemplate.opsForValue().set(key, value, timeout, timeUnit)


    /**
     * Get cached basic object
     *
     * @param key cache key
     * @return Cached object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun <T> getObject(key: String): T? = redisTemplate.opsForValue().get(key) as? T

    /**
     * Delete cached object
     *
     * @param key Cache key
     * @return true=success; false=fail
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun delObject(key: String): Boolean = redisTemplate.delete(key)

    /**
     * Delete list of cached objects
     *
     * @param collection List of cache key
     * @return Number of deleted objects
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun delObject(collection: Collection<String>): Long = redisTemplate.delete(collection)

    /**
     * Cache list of objects
     *
     * @param key Cache key
     * @param dataList List of objects
     * @return Number of cached objects
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun setList(key: String, dataList: List<Any>): Long? = redisTemplate.opsForList().rightPushAll(key, dataList)

    /**
     * Get cached list of objects
     *
     * @param key Cache key
     * @return List of cached objects
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun <T> getList(key: String): List<T>? = redisTemplate.opsForList().range(key, 0, -1) as? List<T>

    /**
     * Cache set of objects
     *
     * @param key Cache key
     * @param dataSet Set of objects
     * @return Bound set operations
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun setSet(key: String, dataSet: Set<Any>): BoundSetOperations<String, Any> {
        val boundSetOps: BoundSetOperations<String, Any> = redisTemplate.boundSetOps(key)
        for (data in dataSet) {
            boundSetOps.add(data)
        }
        return boundSetOps
    }

    /**
     * Get set of cached objects
     *
     * @param key Cache key
     * @return Set of cached object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun <T> getSet(key: String): Set<T>? = redisTemplate.opsForSet().members(key) as? Set<T>

    /**
     * Cache map of objects
     *
     * @param key Cache key
     * @param dataMap Map of objects
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun setMap(key: String, dataMap: Map<String, Any>) = redisTemplate.opsForHash<String, Any>().putAll(key, dataMap)

    /**
     * Get cached map of objects
     *
     * @param key Cache key
     * @return Map of cached objects
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun <T> getMap(key: String): Map<String, T>? =
        redisTemplate.opsForHash<String, Any>().entries(key) as? Map<String, T>

    /**
     * Set value into cached map
     *
     * @param key Cache key
     * @param hKey Map key
     * @param value Value in map
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun setMapValue(key: String, hKey: String, value: Any) =
        redisTemplate.opsForHash<String, Any>().put(key, hKey, value)

    /**
     * Get value in cached map
     *
     * @param key Cache key
     * @param hKey Map key
     * @return Value in map
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun <T> getMapValue(key: String, hKey: String) = redisTemplate.opsForHash<String, T>().get(key, hKey)

    /**
     * Delete value in cached map
     *
     * @param key Cache key
     * @param hKey Map key
     * @return Number of deleted value
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun delMapValue(key: String, hKey: String): Long = redisTemplate.opsForHash<String, Any>().delete(key, hKey)

    /**
     * Get multiple value in cached map
     *
     * @param key Cache key
     * @param hKeys List of map key
     * @return HashMap object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun <T> getMultiMapValue(key: String, hKeys: Collection<String>): List<T> =
        redisTemplate.opsForHash<String, T>().multiGet(key, hKeys)
}
