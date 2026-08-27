package top.fatweb.apimanagement.vo.system

import io.swagger.v3.oas.annotations.media.Schema
import top.fatweb.apimanagement.entity.system.ApiKey

/**
 * API key value object with one-time secret key
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ApiKeyVo
 * @see ApiKey
 */
@Schema(description = "API Key 创建返回参数（含一次性 SecretKey）")
data class ApiKeyWithSecretVo(
    /**
     * API key value object
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiKeyVo
     */
    val apiKey: ApiKeyVo,

    /**
     * One-time secret key
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "SecretKey（仅此一次返回，请妥善保存）")
    val secretKey: String?
)
