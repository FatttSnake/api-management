package top.fatweb.apimanagement.sdk.annotation

import org.springframework.web.bind.annotation.ResponseBody

/**
 * API controller annotation
 *
 * The plugin ID comes from the descriptor, not this annotation, and every mapped method must
 * also carry a stable, non-blank `@Operation(operationId = ...)`, which is the endpoint's
 * persisted identity. A plugin's own `@ControllerAdvice` is never seen.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ResponseBody
 */
@ResponseBody
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ApiController(
    /**
     * API version, must be at least 1
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val version: Int = 1,

    /**
     * Interface sub-path within the plugin namespace, default `""`
     *
     * Leading and trailing slashes are ignored.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val path: String = ""
)
