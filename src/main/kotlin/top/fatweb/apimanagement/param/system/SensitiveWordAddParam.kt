package top.fatweb.apimanagement.param.system

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import top.fatweb.apimanagement.annotation.ParamProcessor
import top.fatweb.apimanagement.entity.system.SensitiveWord

/**
 * Add sensitive word settings parameters
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@ParamProcessor
@Schema(defaultValue = "敏感词添加请求参数")
data class SensitiveWordAddParam(
    /**
     * Word
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "词", required = true)
    @field:NotBlank(message = "Word can not be blank")
    var word: String?,

    /**
     * Use for
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see SensitiveWord.Use
     */
    @field:Schema(description = "用于", allowableValues = ["USERNAME"])
    val useFor: Set<SensitiveWord.Use> = emptySet(),

    /**
     * Enable
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "启用", allowableValues = ["true", "false"], defaultValue = "true")
    val enable: Boolean = true
)
