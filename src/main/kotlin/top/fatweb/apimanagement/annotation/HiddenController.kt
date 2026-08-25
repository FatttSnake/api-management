package top.fatweb.apimanagement.annotation

import io.swagger.v3.oas.annotations.Hidden
import org.springframework.core.annotation.AliasFor
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Hidden controller annotation
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see Hidden
 * @see RequestMapping
 * @see RestController
 */
@Hidden
@RequestMapping
@RestController
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class HiddenController(
    @get:AliasFor(annotation = RequestMapping::class, attribute = "path") val path: Array<String> = [""]
)
