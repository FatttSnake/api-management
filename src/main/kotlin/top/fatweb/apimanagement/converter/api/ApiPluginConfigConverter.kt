package top.fatweb.apimanagement.converter.api

import top.fatweb.apimanagement.component.plugin.PluginConfigFieldType
import top.fatweb.apimanagement.component.plugin.PluginConfigSchema
import top.fatweb.apimanagement.component.plugin.PluginDatasourceSchema
import top.fatweb.apimanagement.vo.api.ApiPluginConfigDatasourceVo
import top.fatweb.apimanagement.vo.api.ApiPluginConfigFieldVo
import top.fatweb.apimanagement.vo.api.ApiPluginConfigGroupVo
import top.fatweb.apimanagement.vo.api.ApiPluginConfigOptionVo

/**
 * Convert to a list of ApiPluginConfigGroupVo objects
 *
 * The declared values are folded into the declaration, so what the administrator's
 * console renders is one payload rather than a schema joined against a list of values.
 *
 * A group also names the datasource it describes, which is what says where a console can
 * offer a connection test. A datasource is composed from fields the gateway holds to one
 * group, and a group to one connection, so the two are found in each other whole or not at
 * all; one naming no slot - a SQLITE datasource - is described by no group.
 *
 * What `value` holds depends on the field, because whether an unset field reads as its
 * declared default depends on the type. Every type but a number reports the default as its
 * value, none of them having a blank appearance to render: a text field, where a blank
 * string is a value rather than an absence, and a boolean or an enumeration, which are drawn
 * as a control that always shows one of its states - a switch, a picker - so leaving one
 * blank on screen would show a state rather than none. A number reads as nothing at all: an
 * empty box says "nothing is set" as plainly as anything, and a blank is what clears one
 * back to its default. `hasValue` is what separates a default being read from the
 * administrator's own value, in either case.
 *
 * @param values Stored settings of the plugin, keyed by setting key
 * @param unreadable Keys of stored secrets the gateway can no longer decrypt
 * @return List of ApiPluginConfigGroupVo objects
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see PluginConfigSchema
 * @see ApiPluginConfigGroupVo
 */
fun PluginConfigSchema.toGroupVo(
    values: Map<String, String>,
    unreadable: Set<String> = emptySet()
): List<ApiPluginConfigGroupVo> =
    groups.map { group ->
        val groupKeys = group.fields.map { it.key }.toSet()

        // A datasource belongs to the group every one of its slots is a field of, and to
        // only that one: the gateway refuses a declaration whose slots are spread over
        // several groups and a group that would describe two, so this finds one whole or
        // not at all - and the empty slot map of a SQLITE datasource matches nothing
        val described = datasources
            .firstOrNull { it.slots.isNotEmpty() && it.slots.values.all { key -> key in groupKeys } }
            ?.name

        ApiPluginConfigGroupVo(
            key = group.key,
            title = group.title,
            description = group.description,
            fields = group.fields.map { field ->
                val secret = field.type == PluginConfigFieldType.SECRET
                val stored = values.containsKey(field.key)

                ApiPluginConfigFieldVo(
                    key = field.key,
                    type = field.type.name,
                    title = field.title,
                    description = field.description,
                    value = when {
                        // A secret is never handed back, however it is stored: leaving the key
                        // out of a submission is what keeps it, and `hasValue` is what says
                        // whether there is one to keep
                        secret -> null

                        // A value of the administrator's own, blank or not
                        stored -> values[field.key]

                        // Nothing is stored, so the declared default is what the plugin reads,
                        // and every type but a number reports it as its value rather than as a
                        // default. A text field, where a blank is a value it can hold, so an
                        // empty box would be saying something else. A boolean and an
                        // enumeration, which are drawn as a control that always shows one of
                        // its states - a switch, a picker - so there is no empty box to draw,
                        // and the state the field would read is the only honest one to show
                        field.type != PluginConfigFieldType.NUMBER -> field.default

                        // A number is the one type with a blank to draw and to submit: an
                        // empty box says nothing is set as plainly as anything, and a blank
                        // is what clears one back to its default
                        else -> null
                    },
                    default = field.default,
                    hasValue = stored,
                    required = field.required,
                    secret = secret,
                    unreadable = field.key in unreadable,
                    placeholder = field.placeholder,
                    minimum = field.minimum,
                    maximum = field.maximum,
                    integer = field.integer,
                    minLength = field.minLength,
                    maxLength = field.maxLength,
                    pattern = field.pattern,
                    options = field.options.orEmpty().map { ApiPluginConfigOptionVo(it.value, it.label) }
                )
            },
            datasource = described
        )
    }

/**
 * Convert to ApiPluginConfigDatasourceVo object
 *
 * @param configured Whether a datasource is configured for the plugin
 * @return ApiPluginConfigDatasourceVo object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see PluginDatasourceSchema
 * @see ApiPluginConfigDatasourceVo
 */
fun PluginDatasourceSchema.toVo(configured: Boolean) = ApiPluginConfigDatasourceVo(
    name = name,
    required = this.required,
    dbType = this.dbType.name,
    configured = configured,
    keys = slots.values.toList()
)
