package top.fatweb.apimanagement.param.system.apiKey

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import top.fatweb.apimanagement.annotation.ParamProcessor
import java.time.LocalDateTime

/**
 * Update API key parameters
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@ParamProcessor
@Schema(description = "API Key 修改请求参数")
data class ApiKeyUpdateParam(
    /**
     * API key ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "Key ID", required = true)
    @field:NotNull(message = "ID can not be null")
    var id: Long?,
    /**
     * Key name
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "Key 名称", example = "my-key")
    var name: String?,
    /**
     * Scoped API codes
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "权限 API 编码列表（属主权限子集）")
    var permissionCodes: List<String>?,
    /**
     * Enable status
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "启用", required = true, example = "true")
    @field:NotNull(message = "Status can not be null")
    var enable: Boolean?,
    /**
     * Expire time
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "过期时间", example = "2030-01-01T00:00:00.000Z")
    var expireTime: LocalDateTime?,
    /**
     * IP whitelist
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "IP 白名单（逗号分隔 IP/CIDR）", example = "10.0.0.0/8,192.168.1.1,2001:db8::/32")
    var ipWhitelist: String?,
    /**
     * Per-key rate limit per minute (0 = inherit the per-user default)
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "每分钟限流次数(0=继承用户默认)", example = "60")
    @field:Min(value = 0, message = "Rate limit must be greater than or equal to 0")
    var rateLimit: Int?,
    /**
     * Optional per-key quota per period (0 = no per-key cap; the per-user quota still applies)
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "单个 Key 周期额度(0=不单独限制，仍受用户配额约束)", example = "100000")
    @field:Min(value = 0, message = "Quota must be greater than or equal to 0")
    var quota: Long?,
    /**
     * Quota period in seconds (null/0 = use the system period)
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "额度周期(秒，0=用系统周期)", example = "86400")
    @field:Min(value = 0, message = "Quota period must be greater than or equal to 0")
    var quotaPeriod: Int?,
    /**
     * Remark
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "备注")
    var remark: String?,
)
