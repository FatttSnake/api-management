package top.fatweb.apimanagement.sdk.annotation

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.core.annotation.AliasFor
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * API controller annotation
 *
 * Marks a class as an API plugin controller. The gateway registers every
 * endpoint under `/api/{plugin}/v{version}/...` and enforces plugin ID / version
 * through ApiVersionCondition; the API-key auth and billing gate is applied
 * by the gateway's access interceptor.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see Tag
 * @see RequestMapping
 * @see RestController
 */
@Tag(name = "")
@RequestMapping
@RestController
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ApiController(
    /**
     * Unique plugin ID (lowercase kebab-case), e.g. "avatar".
     * Namespaces every endpoint of this plugin under /api/{plugin}/v{version}/...
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val plugin: String,

    /**
     * API version
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val version: Int = 1,

    @get:AliasFor(annotation = RestController::class, attribute = "value") val value: String = "",

    /**
     * Interface sub-path within the plugin namespace, default [""]
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @get:AliasFor(annotation = RequestMapping::class, attribute = "path") val path: Array<String> = [""],

    /**
     * Plugin display name; used as the Swagger tag so all versions of a plugin
     * are grouped together, and as the plugin's display name in the permission tree.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @get:AliasFor(annotation = Tag::class, attribute = "name") val pluginName: String,

    @get:AliasFor(annotation = Tag::class, attribute = "description") val description: String
)
