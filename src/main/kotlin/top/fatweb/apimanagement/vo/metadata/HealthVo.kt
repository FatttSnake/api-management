package top.fatweb.apimanagement.vo.metadata

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/**
 * Health value object
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Schema(defaultValue = "接口健康检测返回参数")
data class HealthVo(
    /**
     * Status
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "状态")
    val status: String?,

    /**
     * Timestamp
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see LocalDateTime
     */
    @field:Schema(description = "时间戳", example = "1900-01-01T00:00:00.000Z")
    val timestamp: LocalDateTime?
)
