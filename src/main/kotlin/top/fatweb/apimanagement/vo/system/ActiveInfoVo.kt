package top.fatweb.apimanagement.vo.system

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

/**
 * Active information value object
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Schema(description = "用户活跃信息返回参数")
data class ActiveInfoVo(
    /**
     * Register user number history
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "注册用户数量历史")
    val registerHistory: List<HistoryVo>,

    /**
     * Login user number history
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see HistoryVo
     */
    @field:Schema(description = "登录用户数量历史")
    val loginHistory: List<HistoryVo>,

    /**
     * Verify user number history
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "验证用户数量历史")
    val verifyHistory: List<HistoryVo>
) {
    data class HistoryVo(
        /**
         * Time
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         * @see LocalDate
         */
        @field:Schema(description = "记录时间", example = "1900-01-01")
        val time: LocalDate,

        /**
         * Count
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        @field:Schema(description = "数量")
        val count: Int
    )
}
