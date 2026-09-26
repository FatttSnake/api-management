package top.fatweb.apimanagement.converter.api

import top.fatweb.apimanagement.component.plugin.PluginConfigFieldType
import top.fatweb.apimanagement.component.plugin.PluginConfigSchema
import top.fatweb.apimanagement.component.plugin.PluginConfigSchemaUtil
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
 * A value the administrator never set shows the declared default, with `hasValue` false
 * telling the console it is a default rather than a stored value.
 *
 * @param values Stored settings of the plugin, keyed by setting key
 * @return List of ApiPluginConfigGroupVo objects
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see PluginConfigSchema
 * @see ApiPluginConfigGroupVo
 */
fun PluginConfigSchema.toGroupVo(values: Map<String, String>): List<ApiPluginConfigGroupVo> =
    groups.map { group ->
        ApiPluginConfigGroupVo(
            key = group.key,
            title = group.title,
            description = group.description,
            fields = group.fields.map { field ->
                val secret = field.type == PluginConfigFieldType.SECRET

                ApiPluginConfigFieldVo(
                    key = field.key,
                    type = field.type.name,
                    title = field.title,
                    description = field.description,
                    value = when {
                        // A secret is reported as a mask rather than its value: the mask is
                        // what says "something is stored here", and sending it back is what
                        // says "keep it"
                        secret ->
                            if (values.containsKey(field.key)) PluginConfigSchemaUtil.SECRET_MASK else null

                        else -> values[field.key] ?: field.default
                    },
                    default = field.default,
                    hasValue = values.containsKey(field.key),
                    required = field.required,
                    secret = secret,
                    placeholder = field.placeholder,
                    minimum = field.minimum,
                    maximum = field.maximum,
                    integer = field.integer,
                    minLength = field.minLength,
                    maxLength = field.maxLength,
                    pattern = field.pattern,
                    options = field.options.orEmpty().map { ApiPluginConfigOptionVo(it.value, it.label) }
                )
            }
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
