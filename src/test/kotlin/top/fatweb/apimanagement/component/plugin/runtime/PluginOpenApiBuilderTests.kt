package top.fatweb.apimanagement.component.plugin.runtime

import tools.jackson.databind.json.JsonMapper
import top.fatweb.apimanagement.api.v1.ApiHandler
import top.fatweb.apimanagement.api.v1.ApiResponse
import top.fatweb.apimanagement.api.v1.OperationMetadata
import top.fatweb.apimanagement.api.v1.ParameterLocation
import top.fatweb.apimanagement.api.v1.ParameterSpec
import top.fatweb.apimanagement.api.v1.ResponseSpec
import top.fatweb.apimanagement.api.v1.RouteDefinition
import top.fatweb.apimanagement.api.v1.SchemaSpec
import top.fatweb.apimanagement.plugin.runtime.route.RegisteredRoute
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * OpenAPI fragment builder tests
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see PluginOpenApiBuilder
 */
class PluginOpenApiBuilderTests {
    private val mapper = JsonMapper.builder().build()
    private val builder = PluginOpenApiBuilder(mapper)

    @Test
    fun `parameters carry type format enum description and example`() {
        val route = route(
            OperationMetadata(
                operationId = "link",
                parameters = listOf(
                    ParameterSpec(
                        name = "id",
                        location = ParameterLocation.PATH,
                        type = "string",
                        required = true,
                        description = "文件 ID",
                        example = "1"
                    ),
                    ParameterSpec(
                        name = "mode",
                        location = ParameterLocation.QUERY,
                        type = "string",
                        description = "模式",
                        format = "enum",
                        enumValues = listOf("fast", "safe")
                    )
                )
            )
        )

        val parameters = operation(builder.build("filebox", listOf(route)))["parameters"].asList()
        val parameter = parameters.first().asMap()

        assertEquals("id", parameter["name"])
        assertEquals("path", parameter["in"])
        assertEquals(true, parameter["required"])
        assertEquals("文件 ID", parameter["description"])
        assertEquals("1", parameter["example"])
        assertEquals("string", parameter["schema"].asMap()["type"])

        val schema = parameters[1].asMap()["schema"].asMap()
        assertEquals("enum", schema["format"])
        assertEquals(listOf("fast", "safe"), schema["enum"])
    }

    @Test
    fun `responses carry description schema and example`() {
        val route = route(
            OperationMetadata(
                operationId = "ping",
                responses = mapOf(
                    "200" to ResponseSpec(
                        description = "成功",
                        schema = SchemaSpec.objectOf(
                            properties = mapOf(
                                "message" to SchemaSpec.string("回声消息", example = "pong")
                            ),
                            required = listOf("message")
                        ),
                        example = mapOf("message" to "pong")
                    )
                )
            )
        )

        val response = operation(builder.build("echo", listOf(route)))["responses"]
            .asMap().getValue("200").asMap()

        assertEquals("成功", response["description"])
        val mediaType = response["content"].asMap().getValue("application/json").asMap()
        val schema = mediaType["schema"].asMap()
        val message = schema["properties"].asMap().getValue("message").asMap()

        assertEquals("object", schema["type"])
        assertEquals(listOf("message"), schema["required"])
        assertEquals("string", message["type"])
        assertEquals("回声消息", message["description"])
        assertEquals(listOf("pong"), message["examples"])
        assertEquals("pong", mediaType["example"].asMap()["message"])
    }

    @Test
    fun `request bodies carry a structured schema`() {
        val route = route(
            OperationMetadata(
                operationId = "generateKey",
                requestBodySchema = SchemaSpec.objectOf(
                    properties = mapOf(
                        "code" to SchemaSpec.string("兑换码", example = "abc")
                    ),
                    required = listOf("code")
                )
            ),
            method = "POST"
        )

        val schema = operation(builder.build("jetkey", listOf(route)))["requestBody"]
            .asMap().getValue("content").asMap().getValue("application/json").asMap()
            .getValue("schema").asMap()

        assertEquals("object", schema["type"])
        assertEquals(listOf("code"), schema["required"])
        assertEquals(listOf("abc"), schema["properties"].asMap()["code"].asMap()["examples"])
    }

    private fun route(operation: OperationMetadata, method: String = "GET"): RegisteredRoute =
        RegisteredRoute(
            pluginId = "echo",
            definition = RouteDefinition(method, "/ping", 1, operation),
            handler = ApiHandler { ApiResponse.ok() },
            classLoader = javaClass.classLoader
        )

    @Suppress("UNCHECKED_CAST")
    private fun operation(document: String): Map<String, Any> {
        val root = mapper.readValue(document, Map::class.java) as Map<String, Any>
        val paths = root.getValue("paths") as Map<String, Any>
        val path = paths.values.first() as Map<String, Any>

        return path.values.first() as Map<String, Any>
    }
}

private fun Any?.asMap(): Map<String, Any> = this as Map<String, Any>

private fun Any?.asList(): List<Any> = this as List<Any>
