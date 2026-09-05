package top.fatweb.apimanagement.param.system.api

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import top.fatweb.apimanagement.annotation.ParamProcessor
import top.fatweb.apimanagement.entity.api.ApiInterface
import java.math.BigDecimal

/**
 * Update API interface parameters
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@ParamProcessor
@Schema(description = "API 接口更新请求参数")
data class ApiInterfaceUpdateParam(
    /**
     * API interface ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "API 接口 ID", required = true)
    @field:NotNull(message = "ID can not be null")
    var id: Long?,

    /**
     * Price per call (null = inherit plugin default)
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see BigDecimal
     */
    @field:Schema(description = "每次调用单价 (null=继承插件默认)", example = "0.0100")
    var price: BigDecimal?,

    /**
     * Billing mode
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiInterface.BillingMode
     */
    @field:Schema(
        description = "计费模式",
        allowableValues = ["FREE", "SUCCESS_ONLY", "ALWAYS"],
        example = "SUCCESS_ONLY"
    )
    var billingMode: ApiInterface.BillingMode?,

    /**
     * Need API key status
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "是否需要 API Key", example = "true")
    var needKey: Boolean?,

    /**
     * Rate limit per minute (null = inherit plugin default)
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "每分钟限流次数 (null=继承插件默认)", example = "100")
    var rateLimit: Int?,

    /**
     * Enable status
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "启用", example = "true")
    var enable: Boolean?,

    /**
     * Access mode (null = inherit owning plugin's default)
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiInterface.AccessMode
     */
    @field:Schema(description = "访问模式 (null=继承插件)", allowableValues = ["DEFAULT", "RESTRICTED"])
    var accessMode: ApiInterface.AccessMode?
)
