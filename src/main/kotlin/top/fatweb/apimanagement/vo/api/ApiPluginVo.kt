package top.fatweb.apimanagement.vo.api

import io.swagger.v3.oas.annotations.media.Schema
import tools.jackson.databind.annotation.JsonSerialize
import tools.jackson.databind.ser.std.ToStringSerializer
import top.fatweb.apimanagement.entity.api.ApiInterface
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * API plugin value object
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Schema(description = "API 插件返回参数")
data class ApiPluginVo(
    /**
     * ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:JsonSerialize(using = ToStringSerializer::class)
    val id: Long?,

    /**
     * Unique plugin ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "插件 ID", example = "echo")
    val pluginId: String?,

    /**
     * Plugin display name
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "插件名称", example = "Echo 插件")
    val name: String?,

    /**
     * Description
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "插件描述")
    val description: String?,

    /**
     * Enable status
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "启用", example = "true")
    val enable: Boolean?,

    /**
     * Default price per call
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see BigDecimal
     */
    @field:Schema(description = "默认每次调用单价 (null=免费)", example = "0.0100")
    @field:JsonSerialize(using = ToStringSerializer::class)
    val defaultPrice: BigDecimal?,

    /**
     * Default rate limit per minute
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "默认每分钟限流次数 (null=不限)", example = "100")
    val defaultRateLimit: Int?,

    /**
     * Default access mode inherited by interfaces whose access_mode is null
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiInterface.AccessMode
     */
    @field:Schema(description = "默认访问模式 (接口继承此值)", allowableValues = ["DEFAULT", "RESTRICTED"])
    val defaultAccessMode: ApiInterface.AccessMode?,

    /**
     * Plugin source: BUILT_IN or UPLOADED
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "插件来源", allowableValues = ["BUILT_IN", "UPLOADED"])
    val source: String?,

    /**
     * Plugin version name
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "插件版本名", example = "1.2.0")
    val versionName: String?,

    /**
     * Plugin version code
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "插件版本号 (升级需严格递增)", example = "12")
    val versionCode: Int?,

    /**
     * Original jar file name
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "插件 jar 文件名", example = "echo-1.0.0.jar")
    val jarName: String?,

    /**
     * Signer public key fingerprint
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "签名公钥指纹")
    val signerKeyId: String?,

    /**
     * Last mount error message (null when loaded successfully)
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "最近挂载错误信息")
    val loadError: String?,

    /**
     * Create time
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see LocalDateTime
     */
    @field:Schema(description = "创建时间", example = "2026-01-01T00:00:00.000Z")
    val createTime: LocalDateTime?,

    /**
     * Update time
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see LocalDateTime
     */
    @field:Schema(description = "修改时间", example = "2026-01-01T00:00:00.000Z")
    val updateTime: LocalDateTime?
)
