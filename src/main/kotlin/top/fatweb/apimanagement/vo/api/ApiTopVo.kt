package top.fatweb.apimanagement.vo.api

import io.swagger.v3.oas.annotations.media.Schema
import tools.jackson.databind.annotation.JsonSerialize
import tools.jackson.databind.ser.std.ToStringSerializer
import java.math.BigDecimal

/**
 * API top value object
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Schema(description = "API Top 返回参数")
data class ApiTopVo(
    /**
     * API scoping code
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "API 编码")
    val apiCode: String?,

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
