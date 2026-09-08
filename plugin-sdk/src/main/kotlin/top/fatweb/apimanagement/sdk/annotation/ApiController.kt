package top.fatweb.apimanagement.sdk.annotation

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
 * Plugin-level display metadata (`name` / `description` / `author` / version) is
 * declared once in the plugin descriptor `META-INF/api-plugin.json` (via the
 * Gradle plugin's `apiPlugin { }` DSL) and is NOT repeated here. This annotation
 * only carries the per-controller runtime identity: which plugin this controller
 * belongs to (`plugin`), which API version it declares (`version`) and an optional
 * sub-path fragment (`path`). Per-interface `name` / `description` come from the
 * method's `@Operation` (`summary` / `description`).
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see RequestMapping
 * @see RestController
 */
@RequestMapping
@RestController
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ApiController(
    /**
     * Unique plugin ID (lowercase kebab-case), e.g. "avatar".
     * Namespaces every endpoint of this plugin under /api/{plugin}/v{version}/...
     * and must equal the `pluginId` in `META-INF/api-plugin.json` (the gateway
     * enforces this on install).
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val plugin: String,

    /**
     * API version
     *
     * One plugin may ship several `@ApiController` with different versions side
     * by side for rolling forward compatibility (`/api/{plugin}/v{version}/...`).
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val version: Int = 1,

    /**
     * Interface sub-path within the plugin namespace, default [""]
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @get:AliasFor(annotation = RequestMapping::class, attribute = "path") val path: Array<String> = [""]
)
