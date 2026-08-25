package top.fatweb.apimanagement.vo.metadata

import io.swagger.v3.oas.annotations.media.Schema
import top.fatweb.apimanagement.vo.system.BaseSettingsVo

/**
 * Config information value object
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Schema(defaultValue = "配置信息返回参数")
data class ConfigVo(
    /**
     * System name
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "系统名称")
    val systemName: String?,

    /**
     * Token expiry buffer time(ms)
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "Token 失效缓冲时间（毫秒）")
    val tokenExpiryBufferMs: Long?,

    /**
     * Token expiry check interval time(ms)
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "Token 失效检查周期（毫秒）")
    val tokenExpiryCheckIntervalMs: Long?,

    /**
     * Turnstile site key
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "Turnstile 站点标识")
    val turnstileSiteKey: String?,

    /**
     * Home URL
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "主页 URL")
    val homeUrl: String?
) {
    companion object {
        /**
         * Convert [BaseSettingsVo] to [ConfigVo]
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         * @see BaseSettingsVo
         */
        fun fromBaseSettingsVo(baseSettingsVo: BaseSettingsVo) =
            ConfigVo(
                systemName = baseSettingsVo.systemName ?: "",
                tokenExpiryBufferMs = baseSettingsVo.tokenExpiryBufferMs ?: 0,
                tokenExpiryCheckIntervalMs = baseSettingsVo.tokenExpiryCheckIntervalMs ?: 0,
                turnstileSiteKey = baseSettingsVo.turnstileSiteKey ?: "",
                homeUrl = baseSettingsVo.homeUrl ?: ""
            )
    }
}
