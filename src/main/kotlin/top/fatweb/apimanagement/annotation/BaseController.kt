package top.fatweb.apimanagement.annotation

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.core.annotation.AliasFor
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Base controller annotation
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
annotation class BaseController(
    @get:AliasFor(annotation = RequestMapping::class, attribute = "path") val path: Array<String> = [""],

    @get:AliasFor(annotation = Tag::class, attribute = "name") val name: String,

    @get:AliasFor(annotation = Tag::class, attribute = "description") val description: String
)
