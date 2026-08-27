package top.fatweb.apimanagement.vo.system

import io.swagger.v3.oas.annotations.media.Schema
import tools.jackson.databind.annotation.JsonSerialize
import tools.jackson.databind.ser.std.ToStringSerializer
import top.fatweb.apimanagement.entity.system.SensitiveWord

/**
 * Sensitive word settings value object
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Schema(defaultValue = "敏感词设置返回参数")
data class SensitiveWordVo(
    /**
     * ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:JsonSerialize(using = ToStringSerializer::class)
    val id: Long?,

    /**
     * Word
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "词")
    val word: String?,

    /**
     * Use for
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see SensitiveWord.Use
     */
    @field:Schema(description = "用于")
    val useFor: Set<SensitiveWord.Use>?,

    /**
     * Enable
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "启用")
    val enable: Boolean?
)
