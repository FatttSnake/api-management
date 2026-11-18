package top.fatweb.apimanagement.param.system

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import top.fatweb.apimanagement.annotation.ParamProcessor

/**
 * API platform settings parameters
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@ParamProcessor
@Schema(description = "API 平台设置请求参数")
data class ApiSettingsParam(
    /**
     * Default per-user rate limit per minute
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.2
     */
    @field:Schema(description = "每用户每分钟默认限流次数（所有 Key 与登录态合计；0=不限）", example = "60")
    @field:Min(value = 0, message = "Rate limit must be greater than or equal to 0")
    var defaultUserRateLimitPerMin: Int?,
    /**
     * Default per-IP rate limit per minute for anonymous callers
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.2
     */
    @field:Schema(description = "匿名调用每 IP 每分钟默认限流次数（0=不限）", example = "60")
    @field:Min(value = 0, message = "Rate limit must be greater than or equal to 0")
    var defaultAnonymousRateLimitPerMin: Int?,
    /**
     * Per-IP authentication failure limit per minute
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.2
     */
    @field:Schema(description = "每 IP 每分钟鉴权失败上限（0=关闭；不填默认 30）", example = "30")
    @field:Min(value = 0, message = "Rate limit must be greater than or equal to 0")
    var authFailureLimitPerMin: Int?,
    /**
     * Default per-user quota requests per period
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "每用户每个周期默认额度次数（所有 Key 与登录态合计；0=不限）", example = "100000")
    @field:Min(value = 0, message = "Quota must be greater than or equal to 0")
    var defaultQuota: Long?,
    /**
     * Default per-user quota period in seconds
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "每用户额度周期(秒)", example = "86400")
    @field:Min(value = 1, message = "Quota period must be greater than or equal to 1")
    var defaultQuotaPeriodSeconds: Int?,
    /**
     * Length of generated access key
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "生成的 AccessKey 长度", example = "20")
    @field:Min(value = 8, message = "Length of access key must be greater than or equal to 8")
    var accessKeyLength: Int?,
    /**
     * Length of generated secret key
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "生成的 SecretKey 长度", example = "40")
    @field:Min(value = 16, message = "Length of secret key must be greater than or equal to 16")
    var secretKeyLength: Int?,
    /**
     * Whether balance check is enabled before billing
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "计费前是否校验余额", example = "true")
    var balanceCheckEnabled: Boolean?,
    /**
     * TTL of api key cache in seconds
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "ApiKey 缓存 TTL(秒)", example = "300")
    @field:Min(value = 1, message = "Cache TTL must be greater than or equal to 1")
    var cacheTtlSeconds: Int?,
)
