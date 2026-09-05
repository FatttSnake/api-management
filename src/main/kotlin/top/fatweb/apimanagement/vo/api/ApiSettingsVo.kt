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
     * Default rate limit per minute per key
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "每 Key 每分钟默认限流次数(0=不限)", example = "60")
    val defaultRateLimitPerMin: Int?,

    /**
     * Default quota requests per period per key
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "每 Key 每个周期默认额度次数(0=不限)", example = "100000")
    val defaultQuota: Long?,

    /**
     * Default quota period in seconds
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "默认额度周期(秒)", example = "86400")
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
    val cacheTtlSeconds: Int?
)
