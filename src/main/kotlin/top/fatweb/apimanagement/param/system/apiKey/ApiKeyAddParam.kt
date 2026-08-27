package top.fatweb.apimanagement.param.system.apiKey

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import top.fatweb.apimanagement.annotation.ParamProcessor
import java.time.LocalDateTime

/**
 * Add API key parameters
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@ParamProcessor
@Schema(description = "API Key 添加请求参数")
data class ApiKeyAddParam(
    /**
     * Owner user ID (admin only)
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "所属用户 ID（管理员为指定用户创建 Key）")
    var userId: Long?,

    /**
     * Key name
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "Key 名称", required = true, example = "my-key")
    @field:NotBlank(message = "Name can not be blank")
    var name: String?,

    /**
     * Scoped API codes (subset of the owner account)
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "权限 API 编码列表（属主权限子集，空则快照属主全部）", example = "[\"api:v1:avatar:getRandom\"]")
    var permissionCodes: List<String>?,

    /**
     * Expire time
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see LocalDateTime
     */
    @field:Schema(description = "过期时间", example = "2030-01-01T00:00:00.000Z")
    var expireTime: LocalDateTime?,

    /**
     * IP whitelist
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "IP 白名单（逗号分隔 IP/CIDR，空则不限）", example = "10.0.0.0/8,192.168.1.1")
    var ipWhitelist: String?,

    /**
     * Per-key rate limit per minute
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "每分钟限流次数(0=用全局默认)", example = "60")
    @field:Min(value = 0, message = "Rate limit must be greater than or equal to 0")
    var rateLimit: Int?,

    /**
     * Quota requests per period
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "周期额度次数(0=用全局默认)", example = "100000")
    @field:Min(value = 0, message = "Quota must be greater than or equal to 0")
    var quota: Long?,

    /**
     * Quota period in seconds
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "额度周期(秒)", example = "86400")
    @field:Min(value = 1, message = "Quota period must be greater than or equal to 1")
    var quotaPeriod: Int?,

    /**
     * Remark
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "备注")
    var remark: String?
)
