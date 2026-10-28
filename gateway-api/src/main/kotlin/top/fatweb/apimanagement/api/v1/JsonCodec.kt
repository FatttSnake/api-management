package top.fatweb.apimanagement.api.v1

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
}
