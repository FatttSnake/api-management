package top.fatweb.apimanagement.api.v1

import java.lang.reflect.Type

/**
 * Gateway JSON codec.
 *
 * Plugins use it so that no Jackson type crosses the plugin boundary.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
interface JsonCodec {
    /**
     * Encodes a value as JSON.
     *
     * @param value value
     * @return JSON bytes
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun encode(value: Any?): ByteArray

    /**
     * Decodes JSON bytes.
     *
     * @param json JSON bytes
     * @param type target type
     * @return decoded value
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun <T> decode(json: ByteArray, type: Class<T>): T

    /**
     * Decodes JSON text.
     *
     * @param json JSON text
     * @param type target type
     * @return decoded value
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun <T> decode(json: String, type: Class<T>): T

    /**
     * Decodes JSON bytes into a generic type.
     *
     * @param json JSON bytes
     * @param type target type
     * @return decoded value
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun <T> decode(json: ByteArray, type: Type): T

    /**
     * Decodes JSON text into a generic type.
     *
     * @param json JSON text
     * @param type target type
     * @return decoded value
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun <T> decode(json: String, type: Type): T

    /**
     * Decodes a JSON array.
     *
     * @param json JSON bytes
     * @param elementType element type
     * @return decoded list
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun <T> decodeList(json: ByteArray, elementType: Class<T>): List<T>

    /**
     * Decodes a JSON array.
     *
     * @param json JSON text
     * @param elementType element type
     * @return decoded list
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun <T> decodeList(json: String, elementType: Class<T>): List<T>

    /**
     * Decodes a JSON object into a map.
     *
     * @param json JSON bytes
     * @param keyType key type
     * @param valueType value type
     * @return decoded map
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun <K, V> decodeMap(json: ByteArray, keyType: Class<K>, valueType: Class<V>): Map<K, V>

    /**
     * Decodes a JSON object into a map.
     *
     * @param json JSON text
     * @param keyType key type
     * @param valueType value type
     * @return decoded map
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun <K, V> decodeMap(json: String, keyType: Class<K>, valueType: Class<V>): Map<K, V>
}
