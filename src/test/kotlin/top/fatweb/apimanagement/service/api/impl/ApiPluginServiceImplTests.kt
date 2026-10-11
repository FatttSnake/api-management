package top.fatweb.apimanagement.service.api.impl

import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.GetMapping
import top.fatweb.apimanagement.exception.MissingOperationIdException
import top.fatweb.apimanagement.sdk.annotation.ApiController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Plugin endpoint identity tests
 *
 * Pins the code derivation the mount validates and the request path re-derives, so it cannot
 * start falling back to a method name unnoticed.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ApiPluginServiceImpl
 * @see ApiController
 */
class ApiPluginServiceImplTests {
    @ApiController(plugin = "echo", version = 1)
    private class EchoController {
        @GetMapping
        @Operation(operationId = "ping")
        fun ping(): String = "pong"

        @GetMapping("/unnamed")
        fun unnamed(): String = ""
    }

    private val annotation = EchoController::class.java.getAnnotation(ApiController::class.java)!!

    @Test
    fun `an endpoint's operationId is its code`() {
        val ping = EchoController::class.java.getDeclaredMethod("ping")

        assertEquals("api:echo:v1:ping", ApiPluginServiceImpl.buildCode(annotation, ping))
    }

    @Test
    fun `a mapped method with no operationId is refused rather than falling back to its name`() {
        val unnamed = EchoController::class.java.getDeclaredMethod("unnamed")

        assertFailsWith<MissingOperationIdException> {
            ApiPluginServiceImpl.buildCode(annotation, unnamed)
        }
    }
}
