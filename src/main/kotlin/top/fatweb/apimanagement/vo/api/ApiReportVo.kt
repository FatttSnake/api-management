package top.fatweb.apimanagement.vo.api

import io.swagger.v3.oas.annotations.media.Schema
import tools.jackson.databind.annotation.JsonSerialize
import tools.jackson.databind.ser.std.ToStringSerializer
import top.fatweb.apimanagement.vo.permission.UserWithInfoVo
import java.math.BigDecimal

/**
 * API report value object
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Schema(description = "API 报表返回参数")
data class ApiReportVo(
    /**
     * API key ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:JsonSerialize(using = ToStringSerializer::class)
    val apiKeyId: Long?,

    /**
     * Date (day)
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "日期", example = "2026-08-25")
    val date: String?,

    /**
     * API scoping code
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "API 编码")
    val apiCode: String?,

    /**
     * API name
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "API 名称")
    val apiName: String?,

    /**
     * Request count
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "调用次数")
    val count: Long?,

    /**
     * Billed cost
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see BigDecimal
     */
    @field:Schema(description = "费用")
    @field:JsonSerialize(using = ToStringSerializer::class)
    val cost: BigDecimal?,

    /**
     * API key information
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiKeyVo
     */
    @field:Schema(description = "API Key 信息")
    val keyVo: ApiKeyVo?,

    /**
     * API key owner user information
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see UserWithInfoVo
     */
    @field:Schema(description = "Key 所属用户信息")
    val userVo: UserWithInfoVo?,

    /**
     * API plugin information
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiPluginVo
     */
    @field:Schema(description = "API 插件信息")
    val pluginVo: ApiPluginVo?,

    /**
     * API interface information
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiInterfaceVo
     */
    @field:Schema(description = "API 接口信息")
    val interfaceVo: ApiInterfaceVo?
)
