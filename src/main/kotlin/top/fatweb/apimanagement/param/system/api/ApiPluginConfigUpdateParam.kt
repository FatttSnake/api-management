package top.fatweb.apimanagement.param.system.api

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import top.fatweb.apimanagement.annotation.ParamProcessor

/**
 * Update API plugin config parameters
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@ParamProcessor
@Schema(description = "API 插件配置更新请求参数")
data class ApiPluginConfigUpdateParam(
    /**
     * Plugin ID the configuration belongs to
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "API 插件 ID", required = true, example = "filebox")
    @field:NotBlank(message = "Plugin ID can not be blank")
    var pluginId: String?,

    /**
     * Values to save; a key that is absent is left as it was
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiPluginConfigValueParam
     */
    @field:Schema(description = "配置项")
    @field:Valid
    var values: List<ApiPluginConfigValueParam>?
)

/**
 * Update API plugin config value parameters
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@ParamProcessor
@Schema(description = "API 插件配置项参数")
data class ApiPluginConfigValueParam(
    /**
     * Config key, which the plugin's own schema must declare
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "配置键", required = true, example = "defaultLinkTtlHours")
    @field:NotBlank(message = "Key can not be blank")
    var key: String?,

    /**
     * Config value
     *
     * Empty is a value like any other: it clears the key, deleting the stored row so the
     * plugin reads the declared default - or nothing at all, when the field is a secret,
     * which has no default to fall back to. A secret is kept rather than cleared by
     * sending its mask back, because the ciphertext it is stored as can never be compared
     * against a submission to mean "unchanged".
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "配置值（留空即清除；secret 回传掩码表示保持原值）", example = "2")
    var value: String?
)
