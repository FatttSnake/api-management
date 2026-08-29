package top.fatweb.apimanagement.vo.system

import io.swagger.v3.oas.annotations.media.Schema
import tools.jackson.databind.annotation.JsonSerialize
import tools.jackson.databind.ser.std.ToStringSerializer
import top.fatweb.apimanagement.entity.system.ApiInterface
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * API plugin value object
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Schema(description = "API 插件返回参数")
data class ApiPluginVo(
    /**
     * ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:JsonSerialize(using = ToStringSerializer::class)
    val id: Long?,

    /**
     * Unique plugin ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "插件 ID", example = "avatar")
    val pluginId: String?,

    /**
     * Plugin display name
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "插件名称", example = "随机头像")
    val name: String?,

    /**
     * Description
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "插件描述")
    val description: String?,

    /**
     * Enabled status
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "启用", example = "true")
    val enabled: Boolean?,

    /**
     * Default price per call
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see BigDecimal
     */
    @field:Schema(description = "默认每次调用单价 (null=免费)", example = "0.0100")
    val defaultPrice: BigDecimal?,

    /**
     * Default rate limit per minute
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "默认每分钟限流次数 (null=不限)", example = "100")
    val defaultRateLimit: Int?,

    /**
     * Default access mode inherited by interfaces whose access_mode is null
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiInterface.AccessMode
     */
    @field:Schema(description = "默认访问模式 (接口继承此值)", allowableValues = ["DEFAULT", "RESTRICTED"])
    val defaultAccessMode: ApiInterface.AccessMode?,

    /**
     * Create time
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see LocalDateTime
     */
    @field:Schema(description = "创建时间", example = "2026-01-01T00:00:00.000Z")
    val createTime: LocalDateTime?,

    /**
     * Update time
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see LocalDateTime
     */
    @field:Schema(description = "修改时间", example = "2026-01-01T00:00:00.000Z")
    val updateTime: LocalDateTime?
)
