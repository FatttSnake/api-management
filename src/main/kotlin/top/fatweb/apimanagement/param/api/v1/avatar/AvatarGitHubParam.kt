package top.fatweb.apimanagement.param.api.v1.avatar

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max

/**
 * GitHub style avatar parameters
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see AvatarBaseParam
 */
data class AvatarGitHubParam(
    /**
     * Size of element
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "元素大小", defaultValue = "400")
    @field:Max(1000, message = "Element size must be less than or equal to 1000")
    val elementSize: Int = 400,

    /**
     * Precision of element
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "精确度", defaultValue = "5")
    val precision: Int = 5
) : AvatarBaseParam()
