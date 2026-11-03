package top.fatweb.apimanagement.component.plugin.runtime

import tools.jackson.databind.json.JsonMapper
import top.fatweb.apimanagement.api.v1.JsonCodec
import java.lang.reflect.Type

/**
 * [JsonCodec] backed by the gateway [JsonMapper].
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
internal class JacksonJsonCodec(private val mapper: JsonMapper) : JsonCodec {
    override fun encode(value: Any?): ByteArray = mapper.writeValueAsBytes(value)

    override fun <T> decode(json: ByteArray, type: Class<T>): T = mapper.readValue(json, type)

    override fun <T> decode(json: String, type: Class<T>): T = mapper.readValue(json, type)

    override fun <T> decode(json: ByteArray, type: Type): T = mapper.readValue(json, mapper.constructType(type))

    override fun <T> decode(json: String, type: Type): T = mapper.readValue(json, mapper.constructType(type))

    override fun <T> decodeList(json: ByteArray, elementType: Class<T>): List<T> =
        mapper.readValue(json, mapper.typeFactory.constructCollectionType(List::class.java, elementType))

    override fun <T> decodeList(json: String, elementType: Class<T>): List<T> =
        mapper.readValue(json, mapper.typeFactory.constructCollectionType(List::class.java, elementType))

    override fun <K, V> decodeMap(json: ByteArray, keyType: Class<K>, valueType: Class<V>): Map<K, V> =
        mapper.readValue(json, mapper.typeFactory.constructMapType(Map::class.java, keyType, valueType))

    override fun <K, V> decodeMap(json: String, keyType: Class<K>, valueType: Class<V>): Map<K, V> =
        mapper.readValue(json, mapper.typeFactory.constructMapType(Map::class.java, keyType, valueType))
}
