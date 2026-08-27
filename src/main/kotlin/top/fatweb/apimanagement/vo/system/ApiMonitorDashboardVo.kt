package top.fatweb.apimanagement.vo.system

import io.swagger.v3.oas.annotations.media.Schema

/**
 * API monitor dashboard value object
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Schema(description = "API 监控看板返回参数")
data class ApiMonitorDashboardVo(
    /**
     * Live metrics per API
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiMonitorItemVo
     */
    @field:Schema(description = "实时指标（当前窗口）")
    val live: List<ApiMonitorItemVo>?,

    /**
     * Total requests today
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "今日请求总数")
    val totalToday: Long?,

    /**
     * Error requests today
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "今日错误数")
    val errorToday: Long?,

    /**
     * Active keys this window
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "当前活跃 Key 数")
    val activeKeys: Long?,

    /**
     * Top APIs
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiTopVo
     */
    @field:Schema(description = "Top API")
    val topApis: List<ApiTopVo>?
) {
    /**
     * API monitor item value object
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Schema(description = "API 监控单项返回参数")
    data class ApiMonitorItemVo(
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
        @field:Schema(description = "请求次数")
        val count: Long?,

        /**
         * Error count
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        @field:Schema(description = "错误次数")
        val error: Long?,

        /**
         * Total latency in ms
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        @field:Schema(description = "总耗时(ms)")
        val latencyMs: Long?
    )
}
