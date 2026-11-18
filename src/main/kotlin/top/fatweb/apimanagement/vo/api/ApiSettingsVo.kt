package top.fatweb.apimanagement.vo.api

import io.swagger.v3.oas.annotations.media.Schema

/**
 * API platform settings value object
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Schema(description = "API 平台设置返回参数")
data class ApiSettingsVo(
    /**
     * Default per-user rate limit per minute
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.2
     */
    @field:Schema(description = "每用户每分钟默认限流次数（所有 Key 与登录态合计；0=不限）", example = "60")
    val defaultUserRateLimitPerMin: Int?,
    /**
     * Default per-IP rate limit per minute for anonymous callers
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.2
     */
    @field:Schema(description = "匿名调用每 IP 每分钟默认限流次数（0=不限）", example = "60")
    val defaultAnonymousRateLimitPerMin: Int?,
    /**
     * Per-IP authentication failure limit per minute
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.2
     */
    @field:Schema(description = "每 IP 每分钟鉴权失败上限（0=关闭；默认 30）", example = "30")
    val authFailureLimitPerMin: Int?,
    /**
     * Default per-user quota requests per period
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "每用户每个周期默认额度次数（所有 Key 与登录态合计；0=不限）", example = "100000")
    val defaultQuota: Long?,
    /**
     * Default per-user quota period in seconds
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "每用户额度周期(秒)", example = "86400")
    val defaultQuotaPeriodSeconds: Int?,
    /**
     * Length of generated access key
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "生成的 AccessKey 长度", example = "20")
    val accessKeyLength: Int?,
    /**
     * Length of generated secret key
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "生成的 SecretKey 长度", example = "40")
    val secretKeyLength: Int?,
    /**
     * Whether balance check is enabled before billing
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "计费前是否校验余额", example = "true")
    val balanceCheckEnabled: Boolean?,
    /**
     * TTL of api key cache in seconds
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "ApiKey 缓存 TTL(秒)", example = "300")
    val cacheTtlSeconds: Int?,
)
