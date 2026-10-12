package top.fatweb.apimanagement.service.api.impl

import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.GetMapping
import top.fatweb.apimanagement.exception.MissingOperationIdException
import top.fatweb.apimanagement.sdk.annotation.ApiController
import top.fatweb.apimanagement.sdk.plugin.PluginDescriptor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Plugin endpoint identity tests
 *
 * Pins the code derivation the mount validates and the request path re-derives, and the plugin
 * ID bound.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ApiPluginServiceImpl
 * @see ApiController
 * @see PluginDescriptor
 */
class ApiPluginServiceImplTests {
    @ApiController(version = 1)
    private class EchoController {
        @GetMapping
        @Operation(operationId = "ping")
        fun ping(): String = "pong"

        @GetMapping("/unnamed")
        fun unnamed(): String = ""
    }

    @Test
    fun `an endpoint's operationId is its code`() {
        val ping = EchoController::class.java.getDeclaredMethod("ping")

        assertEquals("api:echo:v1:ping", ApiPluginServiceImpl.buildCode("echo", 1, ping))
    }

    @Test
    fun `a mapped method with no operationId is refused rather than falling back to its name`() {
        val unnamed = EchoController::class.java.getDeclaredMethod("unnamed")

        assertFailsWith<MissingOperationIdException> {
            ApiPluginServiceImpl.buildCode("echo", 1, unnamed)
        }
    }

    @Test
    fun `a plugin ID of 64 characters is accepted and 65 is not`() {
        assertTrue(PluginDescriptor.PLUGIN_ID_REGEX.matches("a".repeat(64)))
        assertFalse(PluginDescriptor.PLUGIN_ID_REGEX.matches("a".repeat(65)))
    }
}
