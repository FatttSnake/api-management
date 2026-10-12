package top.fatweb.apimanagement.sdk.annotation

import org.springframework.web.bind.annotation.ResponseBody

/**
 * API controller annotation
 *
 * Marks a class as an API plugin controller. The gateway registers every endpoint under
 * `/api/{plugin}/v{version}/...` and enforces plugin ID / version through
 * ApiVersionCondition; the API-key auth and billing gate is applied by the gateway's
 * access interceptor. The plugin ID is the descriptor's (`META-INF/api-plugin.json`),
 * not this annotation's.
 *
 * This is deliberately **not** a Spring `@RestController` and carries no `@RequestMapping`
 * meta-annotation. A plugin controller lives in the plugin's own child context and is never
 * detected by the gateway's dispatcher: the gateway scans the jar for this annotation, builds
 * every `RequestMappingInfo` itself and registers it by hand. It therefore needs no
 * `@Controller`, and its base path comes from [path] rather than from `@RequestMapping`. The
 * mapping annotations a method carries (`@GetMapping`, `@PostMapping`, `@PutMapping`,
 * `@PatchMapping`, `@DeleteMapping`) are what that scan reads - a method mapped any other way,
 * notably a bare `@RequestMapping`, is refused at install rather than quietly left unmounted.
 * Their `params` / `headers` / `produces` / `consumes` become routing conditions as declared, and
 * exactly one path is supported: `value` and `path` are aliases, so declare the path through
 * either, and a second element or a disagreement between them is refused rather than ignored.
 *
 * What it *does* borrow from Spring MVC is `@ResponseBody`, and only that: the gateway's shared
 * handler adapter serialises a handler's return value as the response body because the handler
 * class carries it. Without it the return value would be read as a view name and the request
 * would fail the moment it tried to render it. A plugin method therefore returns its
 * `ApiResponse` - or any other serialisable value - directly, and never a view name.
 *
 * What that leaves of the Spring MVC surface inside a plugin:
 * - argument resolution and return serialisation run on the gateway's shared handler adapter,
 *   so `@RequestBody` / `@Valid` / `@RequestParam` / `@PathVariable` and the like work;
 * - annotations the handler itself carries are read off it, so a controller's own
 *   `@ExceptionHandler` / `@InitBinder` / `@ResponseStatus` are honoured;
 * - what does not work is anything contributed by a *separate* bean, because the gateway's
 *   dispatcher looks for those in its own context, not the plugin's: a plugin's
 *   `@ControllerAdvice` is never seen, and neither is a configuration of its that would only
 *   take effect through the gateway's MVC setup.
 *
 * Plugin-level display metadata (`name` / `description` / `author` / version) is declared once
 * in the plugin descriptor `META-INF/api-plugin.json` (via the Gradle plugin's `apiPlugin { }`
 * DSL) and is NOT repeated here. This annotation only carries the per-controller runtime
 * identity: which API version it declares (`version`) and an optional sub-path fragment
 * (`path`). Per-interface `name` / `description` come from the method's `@Operation`
 * (`summary` / `description`).
 *
 * Every mapped method must also carry `@Operation(operationId = "...")` with a non-blank
 * value. That `operationId` is the endpoint's **persisted identity**. It is
 * therefore **required** - a mapped method that omits it, whose code exceeds 200 characters,
 * or whose code duplicates another endpoint's under the same `version` is refused at install -
 * and it is a **breaking field**: changing it makes the endpoint a different one to the
 * gateway, so its previous permission entry is removed and any grant of it is lost. The same
 * `operationId` may be declared once under each version. Keep it stable across versions;
 * `@Operation.summary` / `description` carry the display name and text and are free to change.
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
     * An array to match `@RequestMapping`'s shape, but **only one entry is supported**: the mount
     * uses the first and refuses the install when more are declared, rather than dropping the rest
     * and leaving endpoints that look reachable under every path listed. Split the endpoints across
     * controllers - or across `version`s - if they need different prefixes.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val path: Array<String> = [""]
)
