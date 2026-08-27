package top.fatweb.apimanagement.vo.system

import io.swagger.v3.oas.annotations.media.Schema
import tools.jackson.databind.annotation.JsonSerialize
import tools.jackson.databind.ser.std.ToStringSerializer
import top.fatweb.apimanagement.entity.system.ApiInterface
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * API interface value object
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Schema(description = "API 接口返回参数")
data class ApiInterfaceVo(
    /**
     * ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:JsonSerialize(using = ToStringSerializer::class)
    val id: Long?,

    /**
     * Owning plugin ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "所属插件 ID", example = "avatar")
    val pluginId: String?,

    /**
     * API scoping code
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "API 编码", example = "api:avatar:v1:getRandom")
    val code: String?,

    /**
     * API name
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "API 名称", example = "getRandom")
    val name: String?,

    /**
     * Description
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "API 描述")
    val description: String?,

    /**
     * Request path
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "请求路径", example = "/api/avatar/v1")
    val path: String?,

    /**
     * HTTP method
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "请求方法", example = "GET")
    val method: String?,

    /**
     * API version
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "API 版本", example = "1")
    val apiVersion: Int?,

    /**
     * Price per call
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see BigDecimal
     */
    @field:Schema(description = "每次调用单价 (null=继承插件默认)", example = "0.0100")
    val price: BigDecimal?,

    /**
     * Billing mode
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiInterface.BillingMode
     */
    @field:Schema(description = "计费模式", allowableValues = ["FREE", "SUCCESS_ONLY", "ALWAYS"])
    val billingMode: ApiInterface.BillingMode?,

    /**
     * Need API key status
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "是否需要 API Key", example = "true")
    val needKey: Boolean?,

    /**
     * Rate limit per minute
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "每分钟限流次数 (null=继承插件默认)", example = "100")
    val rateLimit: Int?,

    /**
     * Enabled status
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "启用", example = "true")
    val enabled: Boolean?,

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
