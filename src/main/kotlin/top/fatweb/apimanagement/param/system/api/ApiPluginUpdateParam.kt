package top.fatweb.apimanagement.param.system.api

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import top.fatweb.apimanagement.annotation.ParamProcessor
import top.fatweb.apimanagement.entity.api.ApiInterface
import java.math.BigDecimal

/**
 * Update API plugin parameters
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@ParamProcessor
@Schema(description = "API 插件更新请求参数")
data class ApiPluginUpdateParam(
    /**
     * API plugin ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "API 插件 ID", required = true)
    @field:NotNull(message = "ID can not be null")
    var id: Long?,

    /**
     * Enable status
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "启用", example = "true")
    var enable: Boolean?,

    /**
     * Default price per call (null = free)
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see BigDecimal
     */
    @field:Schema(description = "默认每次调用单价 (null=免费)", example = "0.0100")
    var defaultPrice: BigDecimal?,

    /**
     * Default rate limit per minute (null = unlimited)
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "默认每分钟限流次数 (null=不限)", example = "100")
    var defaultRateLimit: Int?,

    /**
     * Default access mode inherited by interfaces whose access_mode is null
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiInterface.AccessMode
     */
    @field:Schema(description = "默认访问模式 (接口继承此值)", allowableValues = ["DEFAULT", "RESTRICTED"])
    var defaultAccessMode: ApiInterface.AccessMode?
)
