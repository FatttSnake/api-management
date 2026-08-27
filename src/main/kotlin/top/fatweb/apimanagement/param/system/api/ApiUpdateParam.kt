package top.fatweb.apimanagement.param.system.api

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import top.fatweb.apimanagement.annotation.ParamProcessor
import top.fatweb.apimanagement.entity.system.Api
import java.math.BigDecimal

/**
 * Update API parameters
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@ParamProcessor
@Schema(description = "API 更新请求参数")
data class ApiUpdateParam(
    /**
     * API ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "API ID", required = true)
    @field:NotNull(message = "ID can not be null")
    var id: Long?,

    /**
     * Price per call
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see BigDecimal
     */
    @field:Schema(description = "每次调用单价", example = "0.0100")
    var price: BigDecimal?,

    /**
     * Billing mode
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see Api.BillingMode
     */
    @field:Schema(description = "计费模式", allowableValues = ["FREE", "SUCCESS_ONLY", "ALWAYS"], example = "SUCCESS_ONLY")
    var billingMode: Api.BillingMode?,

    /**
     * Need API key status
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "是否需要 API Key", example = "true")
    var needKey: Boolean?,

    /**
     * Per-API rate limit per minute
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "每分钟限流次数(0=不限)", example = "100")
    @field:Min(value = 0, message = "Rate limit must be greater than or equal to 0")
    var rateLimit: Int?,

    /**
     * Enabled status
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "启用", example = "true")
    var enabled: Boolean?
)
