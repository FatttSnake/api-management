package top.fatweb.apimanagement.vo.api

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

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
     * them, constrains them and never sees their secrets, without knowing what a datasource is.
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
    val fields: List<ApiPluginConfigFieldVo>,

    /**
     * Name of the datasource this group describes, or null when it describes none
     *
     * A connection is composed from ordinary config fields rather than stored as one, and
     * the gateway keeps those fields to a single group and that group to a single
     * connection - so a datasource is edited, saved and tested in one piece, and the group
     * is what a console can offer a connection test on. Naming it here is what says which
     * group that is.
     *
     * Null for a group that describes none. A SQLITE datasource declares no slot, so it is
     * described by no group and never appears here.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiPluginConfigDatasourceVo
     */
    @field:Schema(description = "本分组描述的数据源名称，未描述任何数据源时为 null")
    val datasource: String?
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
     * Effective value; a secret is never returned
     *
     * What the field holds, and what a console renders into its form: the administrator's
     * stored value when there is one, and otherwise the declared default for every type but
     * a number - a text field, where a blank string is a value rather than an absence, and a
     * boolean or an enumeration, whose controls have no unset appearance to render. A number
     * has no blank form and therefore no value until one is submitted, and reads as null.
     * [hasValue] is what tells the two apart.
     *
     * A secret comes back as null however it is stored: [hasValue] says whether one is
     * there, leaving the key out of a submission is what keeps it, and a blank clears it.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see hasValue
     */
    @field:Schema(description = "配置值（secret 不回值；未设置时数字项为 null，其余为声明的默认值）", example = "2")
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
     * Whether the stored secret is one the gateway can no longer read
     *
     * Only ever true for a secret that has a value stored: the token secret the gateway
     * encrypts one under was rotated after it was written, and re-entering the value is the
     * only thing that fixes it. Reported rather than raised, so the administrator is told
     * which field to fill in rather than finding out from a plugin that fails to mount.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see secret
     */
    @field:Schema(description = "已存密钥是否已无法解密（token secret 轮换）", example = "false")
    val unreadable: Boolean,

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
