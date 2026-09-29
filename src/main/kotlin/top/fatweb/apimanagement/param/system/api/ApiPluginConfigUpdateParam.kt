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
     * Groups being saved, each with the values it was edited to
     *
     * A group is the unit of saving because it is the unit of editing: the console renders
     * one form per group and saves the one the administrator was working in, so a group
     * left out is one nothing was decided about. That also decides what a save is checked
     * against - the required fields of the groups named here, and no others, since a
     * required field of a group nobody submitted would make the form impossible to fill in.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiPluginConfigGroupParam
     */
    @field:Schema(description = "要保存的配置分组")
    @field:Valid
    var groups: List<ApiPluginConfigGroupParam>?
)

/**
 * Update API plugin config group parameters
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@ParamProcessor
@Schema(description = "API 插件配置分组参数")
data class ApiPluginConfigGroupParam(
    /**
     * Group key, which the plugin's own schema must declare
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "分组键", required = true, example = "share")
    @field:NotBlank(message = "Group key can not be blank")
    var key: String?,

    /**
     * Values of this group to save
     *
     * Every key here has to be one this group declares, so a value cannot be written
     * through a group that does not own it.
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
     * Left out, or null, means the key is not being decided about: the stored value, or the
     * declared default the plugin reads in its absence, stands as it is. A blank one is a
     * value of its own for a free-text field and clears the key everywhere else - a number,
     * a boolean, an enumeration or a secret has no blank form to store, so blanking one of
     * those is how an administrator gets back to the declared default.
     *
     * A secret is never returned by the API: `hasValue` is what says one is stored, leaving
     * the key out keeps it, and a blank clears it.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "配置值（缺省为保持原值，留空为清除）", example = "2")
    var value: String?
)
