package top.fatweb.apimanagement.component.plugin.runtime

import tools.jackson.core.type.TypeReference
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import top.fatweb.apimanagement.api.v1.decode
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Jackson JSON codec tests
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see JacksonJsonCodec
 */
class JacksonJsonCodecTests {
    private val codec = JacksonJsonCodec(JsonMapper.builder().addModule(KotlinModule.Builder().build()).build())

    @Test
    fun `decodeList decodes typed elements from bytes`() {
        val result = codec.decodeList("""[{"id":1,"name":"a"},{"id":2,"name":"b"}]""".toByteArray(), Item::class.java)

        assertEquals(listOf(Item(1, "a"), Item(2, "b")), result)
    }

    @Test
    fun `decodeList decodes typed elements from text`() {
        val result = codec.decodeList("""[{"id":1,"name":"a"}]""", Item::class.java)

        assertEquals(listOf(Item(1, "a")), result)
    }

    @Test
    fun `decodeList returns an empty list`() {
        assertEquals(emptyList<Item>(), codec.decodeList("[]", Item::class.java))
    }

    @Test
    fun `decodeMap decodes typed values from bytes`() {
        val result =
            codec.decodeMap(
                """{"a":{"id":1,"name":"a"}}""".toByteArray(),
                String::class.java,
                Item::class.java,
            )

        assertEquals(mapOf("a" to Item(1, "a")), result)
    }

    @Test
    fun `decodeMap decodes typed values from text`() {
        val result = codec.decodeMap("""{"a":{"id":1,"name":"a"}}""", String::class.java, Item::class.java)

        assertEquals(mapOf("a" to Item(1, "a")), result)
    }

    @Test
    fun `decodeMap returns an empty map`() {
        assertEquals(emptyMap<String, Item>(), codec.decodeMap("{}", String::class.java, Item::class.java))
    }

    @Test
    fun `decode with Type handles nested generics`() {
        val type = object : TypeReference<List<Map<String, Item>>>() {}.type

        val result = codec.decode<List<Map<String, Item>>>("""[{"a":{"id":1,"name":"a"}}]""", type)

        assertEquals(listOf(mapOf("a" to Item(1, "a"))), result)
    }

    @Test
    fun `reified decode resolves a simple type`() {
        val item: Item = codec.decode("""{"id":1,"name":"a"}""")

        assertEquals(Item(1, "a"), item)
    }

    @Test
    fun `reified decode resolves generic types`() {
        val items: List<Item> = codec.decode("""[{"id":1,"name":"a"}]""".toByteArray())

        assertEquals(listOf(Item(1, "a")), items)
    }

    @Test
    fun `reified decode resolves nested generic types`() {
        val value: Map<String, List<Item>> = codec.decode("""{"a":[{"id":1,"name":"a"}]}""")

        assertEquals(mapOf("a" to listOf(Item(1, "a"))), value)
    }

    @Test
    fun `reified decode resolves arrays`() {
        val items: Array<Item> = codec.decode("""[{"id":1,"name":"a"}]""")

        assertEquals(Item(1, "a"), items.single())
    }

    @Test
    fun `encode and decode round trip`() {
        val items = listOf(Item(1, "a"), Item(2, "b"))

        assertEquals(items, codec.decodeList(codec.encode(items), Item::class.java))
    }

    private data class Item(val id: Long, val name: String)
}
