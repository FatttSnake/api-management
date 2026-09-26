package top.fatweb.apimanagement.vo.api

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import top.fatweb.apimanagement.component.plugin.PluginConfigSchemaUtil

/**
 * Plugin config value object
 *
 * The declared schema with the current values folded into it, so the administrator's
 * console renders a form from one payload instead of joining a declaration against a
 * separate list of values.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Schema(description = "API 插件配置返回参数")
data class ApiPluginConfigVo(
    /**
     * Plugin ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "API 插件 ID", example = "filebox")
    val pluginId: String?,

    /**
     * Config groups; empty when the plugin declares no configuration
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiPluginConfigGroupVo
     */
    @field:Schema(description = "配置分组")
    val groups: List<ApiPluginConfigGroupVo>,

    /**
     * Datasource declarations; empty when the plugin uses no database of its own
     *
     * Their connection settings are ordinary declared fields and arrive in [groups] with
     * every other one, which is the whole point of declaring them: the console renders
     * them, constrains them and masks their secrets without knowing what a datasource is.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiPluginConfigDatasourceVo
     */
    @field:Schema(description = "数据源声明")
    val datasources: List<ApiPluginConfigDatasourceVo>
)

/**
 * Plugin config group value object
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Schema(description = "API 插件配置分组返回参数")
data class ApiPluginConfigGroupVo(
    /**
     * Group key
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "分组键", example = "share")
    val key: String,

    /**
     * Display title
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "分组标题")
    val title: String?,

    /**
     * Display description
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "分组描述")
    val description: String?,

    /**
     * Fields of this group
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiPluginConfigFieldVo
     */
    @field:Schema(description = "配置项")
    val fields: List<ApiPluginConfigFieldVo>
)

/**
 * Plugin config field value object
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Schema(description = "API 插件配置项返回参数")
data class ApiPluginConfigFieldVo(
    /**
     * Config key
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "配置键", example = "defaultLinkTtlHours")
    val key: String,

    /**
     * Field type: STRING / TEXT / NUMBER / BOOLEAN / ENUM / SECRET
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(
        description = "配置类型",
        allowableValues = ["STRING", "TEXT", "NUMBER", "BOOLEAN", "ENUM", "SECRET"],
        example = "NUMBER"
    )
    val type: String,

    /**
     * Display title
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "配置标题")
    val title: String?,

    /**
     * Display description
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "配置描述")
    val description: String?,

    /**
     * Effective value; a secret is masked rather than returned
     *
     * The stored or declared value of an ordinary field. A secret never comes back: a set
     * one is reported as [PluginConfigSchemaUtil.SECRET_MASK], which the console sends back
     * untouched to mean "keep it", replacing it with any other value and clearing it with a
     * blank one. An unset secret reports null like any other unset field.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "配置值（secret 回掩码，不回明文）", example = "2")
    val value: String?,

    /**
     * Value used when the administrator has not set one
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "默认值", example = "1")
    val default: String?,

    /**
     * Whether the administrator has set a value of their own
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "是否已被管理员设置", example = "false")
    val hasValue: Boolean,

    /**
     * Whether the value is required
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "是否必填", example = "false")
    val required: Boolean,

    /**
     * Whether the value is a secret
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "是否为密钥项", example = "false")
    val secret: Boolean,

    /**
     * Input placeholder
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "输入提示")
    val placeholder: String?,

    /**
     * Lowest accepted value of a number field
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "最小值", example = "1")
    val minimum: BigDecimal?,

    /**
     * Highest accepted value of a number field
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "最大值", example = "168")
    val maximum: BigDecimal?,

    /**
     * Whether a number field rejects a fractional part
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "是否为整数", example = "true")
    val integer: Boolean,

    /**
     * Lowest accepted length of a text field
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "最小长度", example = "1")
    val minLength: Int?,

    /**
     * Highest accepted length of a text field
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "最大长度", example = "200")
    val maxLength: Int?,

    /**
     * Regular expression a text field must match
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "校验正则")
    val pattern: String?,

    /**
     * Allowed values of an enum field
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiPluginConfigOptionVo
     */
    @field:Schema(description = "枚举可选值")
    val options: List<ApiPluginConfigOptionVo>
)

/**
 * Plugin config option value object
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Schema(description = "API 插件配置枚举项返回参数")
data class ApiPluginConfigOptionVo(
    /**
     * Value stored when the option is chosen
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "选项值", example = "fast")
    val value: String,

    /**
     * Display label
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "选项名称", example = "快速")
    val label: String?
)

/**
 * Plugin config datasource value object
 *
 * The state of one declared datasource, as opposed to its settings: those are ordinary
 * config fields and are already in the groups. What is here is what cannot be read off
 * the form - which datasources exist, what they are, and whether each is ready to be
 * connected to.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Schema(description = "API 插件配置数据源声明返回参数")
data class ApiPluginConfigDatasourceVo(
    /**
     * Name the plugin refers to this datasource by
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "数据源名称", example = "main")
    val name: String,

    /**
     * Whether the plugin cannot run without a datasource
     *
     * Only ever unsatisfied by a MySQL datasource: a SQLite one is supplied by the
     * gateway as soon as it is declared
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "是否必须配置", example = "true")
    val required: Boolean,

    /**
     * Database type, which decides what the console shows
     *
     * A SQLite datasource is supplied by the gateway, so the console presents it as
     * read-only; a MySQL one is the administrator's to fill in. One byte of payload is
     * what keeps the console from having to guess that from the connection settings.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "数据库类型", allowableValues = ["MYSQL", "SQLITE"], example = "MYSQL")
    val dbType: String,

    /**
     * Whether this datasource is ready to be connected to
     *
     * Answered from the stored configuration rather than by connecting to anything, so
     * a datasource whose server is down still reports as configured
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "是否已配置", example = "false")
    val configured: Boolean,

    /**
     * Config keys holding this datasource's connection facts
     *
     * What tells a client which of the declared fields belong to this datasource - so a
     * connection can be tried with the values currently in the form, before they are
     * saved, without the client having to know how a slot is named.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "该数据源对应的配置项 key", example = "[\"db.host\", \"db.port\"]")
    val keys: List<String>
)
