package top.fatweb.apimanagement.param.api.v1.avatar

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Pattern

/**
 * Avatar base parameters
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
open class AvatarBaseParam {
    /**
     * Seed to generate avatar
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Schema(description = "种子")
    var seed: Long? = null

    /**
     * Size of image
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Schema(description = "图像大小", defaultValue = "128")
    @Max(256, message = "Size must be less than or equal to 256")
    var size: Int? = null

    /**
     * Margin of image
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Schema(description = "外边距", defaultValue = "0")
    var margin: Int? = null

    /**
     * Padding of image
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Schema(description = "内边距", defaultValue = "0")
    var padding: Int? = null

    /**
     * List of colors to generate avatar
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Schema(description = "颜色列表", example = "#FFFFFFAA")
    var colors: List<String>? = null

    /**
     * Background of image
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Schema(description = "背景颜色", example = "#FFFFFFAA")
    @Pattern(regexp = "^#[0-9a-fA-F]{6}|#[0-9a-fA-F]{8}$", message = "Background color must be a hex color code")
    var background: String? = null
}
