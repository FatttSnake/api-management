package fixture.plugin

import top.fatweb.apimanagement.api.v1.ApiHandler
import top.fatweb.apimanagement.api.v1.ApiPlugin
import top.fatweb.apimanagement.api.v1.ApiRequest
import top.fatweb.apimanagement.api.v1.ApiResponse
import top.fatweb.apimanagement.api.v1.GatewayContext
import top.fatweb.apimanagement.api.v1.OperationMetadata
import top.fatweb.apimanagement.api.v1.ParameterSpec
import top.fatweb.apimanagement.api.v1.RouteDefinition

class PingHandler(private val pluginId: String) : ApiHandler {
    override fun handle(request: ApiRequest): ApiResponse =
        ApiResponse.ok(
            mapOf(
                "message" to "hello, ${request.pathParam("name")}",
                "plugin" to pluginId
            )
        )
}

class FixtureApiPlugin : ApiPlugin {
    override fun onStart(context: GatewayContext) {
        context.api().register(
            RouteDefinition(
                method = "GET",
                path = "/ping/{name}",
                apiVersion = 1,
                operation = OperationMetadata(
                    operationId = "ping",
                    summary = "Ping",
                    parameters = listOf(ParameterSpec.path("name"))
                )
            ),
            PingHandler(context.metadata().pluginId)
        )
    }
}
