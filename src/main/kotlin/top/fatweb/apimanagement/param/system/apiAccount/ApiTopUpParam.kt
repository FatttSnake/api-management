package top.fatweb.apimanagement.param.system.apiAccount

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotNull
import top.fatweb.apimanagement.annotation.ParamProcessor
import java.math.BigDecimal

/**
 * API account top-up parameters
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@ParamProcessor
@Schema(description = "API 账户充值请求参数")
data class ApiTopUpParam(
    /**
     * Target user ID (admin only)
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "充值目标用户 ID（管理员为指定用户充值）")
    var userId: Long?,

    /**
     * Amount to top up
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see BigDecimal
     */
    @field:Schema(description = "充值金额", required = true, example = "100.0000")
    @field:NotNull(message = "Amount can not be null")
    @field:DecimalMin(value = "0.0001", message = "Amount must be greater than 0")
    var amount: BigDecimal?,

    /**
     * Top-up order number for idempotency
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "充值订单号（幂等）", example = "ORDER-20260825-001")
    var orderNo: String?,

    /**
     * Remark
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "备注")
    var remark: String?
)
