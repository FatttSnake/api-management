package top.fatweb.apimanagement.vo.api

import io.swagger.v3.oas.annotations.media.Schema
import tools.jackson.databind.annotation.JsonSerialize
import tools.jackson.databind.ser.std.ToStringSerializer
import java.time.LocalDateTime

/**
 * API key value object
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Schema(description = "API Key 返回参数")
data class ApiKeyVo(
    /**
     * ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:JsonSerialize(using = ToStringSerializer::class)
    val id: Long?,
    /**
     * Owner user ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:JsonSerialize(using = ToStringSerializer::class)
    val userId: Long?,
    /**
     * Access key
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "AccessKey", example = "AbC123...")
    val accessKey: String?,
    /**
     * Key name
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "Key 名称", example = "my-key")
    val name: String?,
    /**
     * Scoped API codes
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "权限 API 编码列表", example = "[\"api:echo:v1:ping\"]")
    val permissions: List<String>?,
    /**
     * Enabled status
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "启用", example = "true")
    val enable: Boolean?,
    /**
     * Expire time
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "过期时间", example = "2030-01-01T00:00:00.000Z")
    val expireTime: LocalDateTime?,
    /**
     * IP whitelist
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "IP 白名单", example = "10.0.0.0/8,192.168.1.1")
    val ipWhitelist: String?,
    /**
     * Per-key rate limit per minute (0 = inherit the per-user default)
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "每分钟限流次数(0=继承用户默认)", example = "60")
    val rateLimit: Int?,
    /**
     * Optional per-key quota per period (0 = no per-key cap; the per-user quota still applies)
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "单个 Key 周期额度(0=不单独限制，仍受用户配额约束)", example = "100000")
    val quota: Long?,
    /**
     * Quota period in seconds (null/0 = use the system period)
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "额度周期(秒，0=用系统周期)", example = "86400")
    val quotaPeriod: Int?,
    /**
     * Last used time
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "最后使用时间", example = "2026-01-01T00:00:00.000Z")
    val lastUsedTime: LocalDateTime?,
    /**
     * Remark
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "备注")
    val remark: String?,
    /**
     * Create time
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "创建时间", example = "2026-01-01T00:00:00.000Z")
    val createTime: LocalDateTime?,
    /**
     * Update time
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "修改时间", example = "2026-01-01T00:00:00.000Z")
    val updateTime: LocalDateTime?,
)
