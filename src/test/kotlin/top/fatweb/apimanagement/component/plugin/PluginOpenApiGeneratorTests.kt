package top.fatweb.apimanagement.component.plugin

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.method.RequestMappingInfo
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import top.fatweb.apimanagement.sdk.annotation.ApiController
import top.fatweb.apimanagement.sdk.plugin.ApiResponse
import java.lang.reflect.Method
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Plugin OpenAPI generator tests
 *
 * Pins the fragment the user-facing documentation is rendered from: the paths and operation
 * identities the mount produced, and the parameter, body and response shapes read back off
 * the handlers. Endpoints are built directly rather than by mounting a jar, which is what the
 * generator is given anyway.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see PluginOpenApiGenerator
 * @see MountedEndpoint
 */
class PluginOpenApiGeneratorTests {
    private val mapper = JsonMapper.builder().build()
    private val generator = PluginOpenApiGenerator(mapper)

    @Test
    fun `a path item is keyed by the mounted path and method`() {
        val json = generate(item("GET"))

        val operation = json.path("paths").path("/api/test/v1/items/{id}").path("get")
        assertEquals("getItem", operation.path("operationId").asString())
        assertEquals("获取条目", operation.path("summary").asString())
        assertEquals("按 ID 获取", operation.path("description").asString())
    }

    @Test
    fun `a nullable scalar is a type union and keeps its format`() {
        val item = schema("Item")

        assertEquals(listOf("integer", "null"), item.typeNames("size"))
        assertEquals("int64", item.path("properties").path("size").path("format").asString())
    }

    @Test
    fun `a non-nullable scalar carries no null and joins required`() {
        val item = schema("Item")

        assertEquals(listOf("string"), item.typeNames("name"))
        assertTrue(item.path("required").values().map { it.asString() }.contains("name"))
        assertFalse(item.path("required").values().map { it.asString() }.contains("size"))
    }

    @Test
    fun `a scalar carries the description and example its property declares`() {
        val name = schema("Item").path("properties").path("name")

        assertEquals("名称", name.path("description").asString())
        assertEquals(listOf("foo"), name.path("examples").values().map { it.asString() })
    }

    @Test
    fun `an example keeps the type its schema describes`() {
        val properties = schema("Item").path("properties")

        // Swagger declares every example as a string; a numeric one against an integer schema
        // would be a mismatch a validator flags, and reads as "20480" rather than 20480
        val bytes = properties.path("bytes").path("examples").get(0)
        assertTrue(bytes.isNumber)
        assertEquals(20480L, bytes.asLong())

        assertTrue(properties.path("hit").path("examples").get(0).isBoolean)
    }

    @Test
    fun `a collection is an array of its element`() {
        val item = schema("Item")

        assertEquals(listOf("array"), item.typeNames("tags"))
        assertEquals(listOf("string"), item.path("properties").path("tags").path("items").typeNames())
    }

    @Test
    fun `an enum is a string with its constants`() {
        val kind = schema("Item").path("properties").path("kind")

        assertEquals(listOf("string"), kind.typeNames())
        assertEquals(listOf("A", "B"), kind.path("enum").values().map { it.asString() })
    }

    @Test
    fun `a date and a uuid keep their formats`() {
        val properties = schema("Item").path("properties")

        assertEquals("date-time", properties.path("createdAt").path("format").asString())
        assertEquals("uuid", properties.path("id").path("format").asString())
        assertEquals(listOf("number"), schema("Item").typeNames("price"))
    }

    @Test
    fun `a self reference resolves to the schema it is already registered under`() {
        val item = schema("Item")

        val child = item.path("properties").path("child")
        assertEquals("#/components/schemas/Item", child.path("anyOf").get(0).path("\$ref").asString())
        assertEquals("null", child.path("anyOf").get(1).path("type").asString())
    }

    @Test
    fun `the response envelope is inlined around the payload it wraps`() {
        val schema = successSchema()

        assertEquals("object", schema.path("type").asString())
        val data = schema.path("properties").path("data")
        assertEquals("#/components/schemas/Item", data.path("anyOf").get(0).path("\$ref").asString())

        // Inlined rather than referenced: one ApiResponse name cannot stand for two payloads
        val json = generate(item("GET"))
        assertFalse(json.path("components").path("schemas").has("ApiResponse"))
    }

    @Test
    fun `bound parameters are described by location and requiredness`() {
        val parameters = generate(item("GET")).path("paths").path("/api/test/v1/items/{id}").path("get").path("parameters")

        val id = parameters.values().first { it.path("name").asString() == "id" }
        assertEquals("path", id.path("in").asString())
        assertTrue(id.path("required").asBoolean())

        val verbose = parameters.values().first { it.path("name").asString() == "verbose" }
        assertEquals("query", verbose.path("in").asString())
        assertFalse(verbose.path("required").asBoolean())
        assertEquals(listOf("boolean"), verbose.path("schema").typeNames())

        // Named off the Kotlin parameter, since the bytecode carries no parameter names
        assertTrue(parameters.values().any { it.path("name").asString() == "tag" })
    }

    @Test
    fun `a json body is described from its argument`() {
        val json = generate(endpoint("create", "POST"))

        val body = json.path("paths").path("/api/test/v1/items").path("post").path("requestBody")
        assertTrue(body.path("required").asBoolean())
        assertEquals(
            "#/components/schemas/Item",
            body.path("content").path("application/json").path("schema").path("\$ref").asString()
        )
    }

    @Test
    fun `a multipart handler describes each part`() {
        val json = generate(endpoint("upload", "POST"))

        val body = json.path("paths").path("/api/test/v1/upload").path("post").path("requestBody")
        val schema = body.path("content").path("multipart/form-data").path("schema")
        assertEquals("object", schema.path("type").asString())

        // A part is bytes: the 3.1 spelling of a binary payload, the same one a byte-array
        // response gets, rather than 3.0's `format: binary`
        val file = schema.path("properties").path("file")
        assertEquals(listOf("string"), file.typeNames())
        assertEquals("application/octet-stream", file.path("contentMediaType").asString())
        assertEquals(
            "#/components/schemas/Item",
            schema.path("properties").path("meta").path("\$ref").asString()
        )
    }

    @Test
    fun `a handler that returns nothing has no response content`() {
        val operation = generate(endpoint("delete", "DELETE")).path("paths").path("/api/test/v1/items/{id}").path("delete")

        assertEquals("OK", operation.path("responses").path("200").path("description").asString())
        assertFalse(operation.path("responses").path("200").has("content"))
        assertFalse(operation.has("requestBody"))
        assertTrue(operation.path("parameters").values().any { it.path("name").asString() == "id" })
    }

    @Test
    fun `a declared error response is kept beside the success one`() {
        val responses = generate(endpoint("find", "GET")).path("paths")
            .path("/api/test/v1/find").path("get").path("responses")

        assertEquals("未找到", responses.path("404").path("description").asString())
    }

    /**
     * Build the fragment of one endpoint
     *
     * @param endpoint Endpoint to describe
     * @return Parsed fragment
     */
    private fun generate(endpoint: MountedEndpoint): JsonNode = mapper.readTree(generator.generate(listOf(endpoint)))

    private fun endpoint(methodName: String, httpMethod: String): MountedEndpoint {
        val method = method(methodName)
        val path = pathOf(methodName)

        return MountedEndpoint(
            controller = TestController(),
            method = method,
            code = "api:test:v1:$methodName",
            name = "",
            description = "",
            fullPath = path,
            httpMethod = httpMethod,
            apiVersion = 1,
            info = RequestMappingInfo.paths(path).methods(RequestMethod.valueOf(httpMethod)).build()
        )
    }

    private fun method(name: String): Method = TestController::class.java.declaredMethods.first { it.name == name }

    private fun pathOf(name: String): String = when (name) {
        "getItem" -> "/api/test/v1/items/{id}"
        "create" -> "/api/test/v1/items"
        "upload" -> "/api/test/v1/upload"
        "delete" -> "/api/test/v1/items/{id}"
        else -> "/api/test/v1/find"
    }

    /**
     * Read the success response schema of the `getItem` endpoint
     *
     * @return Response schema
     */
    private fun successSchema(): JsonNode =
        generate(item("GET")).path("paths").path("/api/test/v1/items/{id}").path("get")
            .path("responses").path("200").path("content").path("application/json").path("schema")

    private fun item(httpMethod: String): MountedEndpoint = endpoint("getItem", httpMethod)

    /**
     * Read a registered schema
     *
     * @param name Schema name
     * @return Schema node
     */
    private fun schema(name: String): JsonNode = generate(item("GET")).path("components").path("schemas").path(name)

    private fun JsonNode.typeNames(name: String? = null): List<String> {
        val type = if (name == null) path("type") else path("properties").path(name).path("type")

        return if (type.isArray) type.values().map { it.asString() } else listOf(type.asString())
    }

    @ApiController(version = 1)
    private class TestController {
        @GetMapping("/items/{id}")
        @Operation(operationId = "getItem", summary = "获取条目", description = "按 ID 获取")
        fun getItem(
            @PathVariable id: String,
            @RequestParam(name = "verbose", required = false) verbose: Boolean,
            @RequestParam(required = false) tag: String?
        ): ApiResponse<Item> = ApiResponse.ok()

        @PostMapping("/items")
        @Operation(operationId = "create")
        fun create(@RequestBody body: Item): ApiResponse<Item> = ApiResponse.ok(body)

        @PostMapping("/upload")
        @Operation(operationId = "upload")
        fun upload(
            @RequestPart("file") file: MultipartFile,
            @RequestPart("meta") meta: Item
        ): ApiResponse<String> = ApiResponse.ok("ok")

        @DeleteMapping("/items/{id}")
        @Operation(operationId = "delete")
        fun delete(@PathVariable id: String) {
        }

        @GetMapping("/find")
        @Operation(operationId = "find")
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "未找到")
        fun find(): ApiResponse<Item> = ApiResponse.ok()
    }

    private enum class Kind { A, B }

    private data class Item(
        @field:Schema(description = "名称", example = "foo") val name: String,
        @field:Schema(description = "字节数", example = "20480") val bytes: Long,
        @field:Schema(description = "命中", example = "true") val hit: Boolean,
        val size: Long?,
        val tags: List<String>,
        val kind: Kind,
        val createdAt: LocalDateTime,
        val id: UUID,
        val price: BigDecimal,
        val child: Item?
    )
}
