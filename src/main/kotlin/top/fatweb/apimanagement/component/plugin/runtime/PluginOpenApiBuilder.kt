package top.fatweb.apimanagement.component.plugin.runtime

import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import top.fatweb.apimanagement.plugin.runtime.route.RegisteredRoute

/**
 * Builds the OpenAPI fragment stored on the plugin row.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Component
class PluginOpenApiBuilder(
    private val jsonMapper: JsonMapper
) {
    /**
     * Builds an OpenAPI 3.1 document from plugin routes.
     *
     * @param pluginId plugin ID
     * @param routes registered routes
     * @return OpenAPI JSON
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun build(pluginId: String, routes: List<RegisteredRoute>): String {
        val paths = linkedMapOf<String, MutableMap<String, Any>>()
        routes.forEach { route ->
            val definition = route.definition
            val path = "/api/$pluginId/v${definition.apiVersion}${definition.path}"
            val operation = linkedMapOf(
                "operationId" to definition.operation.operationId,
                "summary" to definition.operation.summary,
                "description" to definition.operation.description,
                "responses" to responsesOf(definition.operation.responses)
            )
            if (definition.operation.parameters.isNotEmpty()) {
                operation["parameters"] = definition.operation.parameters.map { parameter ->
                    linkedMapOf(
                        "name" to parameter.name,
                        "in" to parameter.location.name.lowercase(),
                        "required" to parameter.required,
                        "description" to parameter.description,
                        "schema" to mapOf("type" to parameter.type)
                    )
                }
            }
            definition.operation.requestBodySchema?.let { schema ->
                operation["requestBody"] = mapOf(
                    "content" to mapOf(
                        "application/json" to mapOf(
                            "schema" to parseSchema(schema)
                        )
                    )
                )
            }
            if (definition.operation.tags.isNotEmpty()) {
                operation["tags"] = definition.operation.tags
            }
            paths.getOrPut(path) { linkedMapOf() }[definition.method.lowercase()] = operation
        }

        val document = linkedMapOf<String, Any>(
            "openapi" to "3.1.0",
            "paths" to paths
        )
        return jsonMapper.writeValueAsString(document)
    }

    private fun responsesOf(schemas: Map<String, String>): Map<String, Any> {
        if (schemas.isEmpty()) {
            return mapOf("200" to mapOf("description" to "OK"))
        }
        return schemas.mapValues { (_, schema) ->
            mapOf(
                "description" to "OK",
                "content" to mapOf(
                    "application/json" to mapOf(
                        "schema" to parseSchema(schema)
                    )
                )
            )
        }
    }

    private fun parseSchema(schema: String): Any =
        runCatching { jsonMapper.readValue(schema, Map::class.java) }
            .getOrElse { mapOf("type" to "object") }
}
