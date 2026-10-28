package top.fatweb.apimanagement.component.plugin.runtime

import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import top.fatweb.apimanagement.api.v1.ParameterSpec
import top.fatweb.apimanagement.api.v1.ResponseSpec
import top.fatweb.apimanagement.api.v1.SchemaSpec
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
            val operation = linkedMapOf<String, Any>(
                "operationId" to definition.operation.operationId,
                "summary" to definition.operation.summary,
                "description" to definition.operation.description,
                "responses" to responsesOf(definition.operation.responses)
            )
            if (definition.operation.parameters.isNotEmpty()) {
                operation["parameters"] = parametersOf(definition.operation.parameters)
            }
            definition.operation.requestBodySchema?.let { schema ->
                operation["requestBody"] = mapOf(
                    "content" to mapOf(
                        "application/json" to mapOf(
                            "schema" to schemaOf(schema)
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

    private fun parametersOf(parameters: List<ParameterSpec>): List<Map<String, Any>> =
        parameters.map { parameter ->
            val schema = linkedMapOf<String, Any>("type" to parameter.type)
            parameter.format?.takeIf { it.isNotBlank() }?.let { schema["format"] = it }
            if (parameter.enumValues.isNotEmpty()) {
                schema["enum"] = parameter.enumValues
            }

            val node = linkedMapOf<String, Any>(
                "name" to parameter.name,
                "in" to parameter.location.name.lowercase(),
                "required" to parameter.required,
                "schema" to schema
            )
            if (parameter.description.isNotBlank()) {
                node["description"] = parameter.description
            }
            parameter.example?.let { node["example"] = it }
            node
        }

    private fun responsesOf(responses: Map<String, ResponseSpec>): Map<String, Any> {
        if (responses.isEmpty()) {
            return mapOf("200" to mapOf("description" to "OK"))
        }
        return responses.mapValues { (_, response) ->
            val mediaType = linkedMapOf<String, Any>()
            response.schema?.let { mediaType["schema"] = schemaOf(it) }
            response.example?.let { mediaType["example"] = it }

            val node = linkedMapOf<String, Any>("description" to response.description)
            if (mediaType.isNotEmpty()) {
                node["content"] = mapOf("application/json" to mediaType)
            }
            node
        }
    }

    private fun schemaOf(spec: SchemaSpec): Map<String, Any> {
        val node = linkedMapOf<String, Any>(
            "type" to if (spec.nullable) listOf(spec.type, "null") else spec.type
        )
        if (spec.description.isNotBlank()) {
            node["description"] = spec.description
        }
        spec.format?.takeIf { it.isNotBlank() }?.let { node["format"] = it }
        if (spec.enumValues.isNotEmpty()) {
            node["enum"] = spec.enumValues
        }
        spec.example?.let { node["examples"] = listOf(it) }
        if (spec.properties.isNotEmpty()) {
            node["properties"] = spec.properties.mapValues { (_, property) -> schemaOf(property) }
        }
        if (spec.required.isNotEmpty()) {
            node["required"] = spec.required
        }
        spec.items?.let { node["items"] = schemaOf(it) }

        return node
    }
}
