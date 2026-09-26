package top.fatweb.apimanagement.component.plugin

import tools.jackson.databind.json.JsonMapper
import java.math.BigDecimal

/**
 * Plugin config schema util
 *
 * Owns the declared format of `META-INF/plugin-config.json` and every check applied to
 * a value before it is stored. Both halves are deliberately pure: the gateway parses
 * the file while mounting a plugin and the admin API validates submitted values with
 * the same rules, so neither needs Spring to be tested.
 *
 * Parsing is strict on purpose. A schema that cannot be honoured (an unknown type, a
 * duplicate key, a constraint on a type that does not support it) is an authoring
 * mistake that would otherwise surface as a setting the administrator can see but the
 * plugin never reads, so it is rejected instead.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
object PluginConfigSchemaUtil {
    /**
     * Value standing in for a stored secret
     *
     * A secret is encrypted at rest and never returned by the admin API, so a console
     * cannot answer "is this still the value I loaded" by comparing values. It is handed
     * this mask instead: sending it back means "keep the stored one", any other value
     * replaces it, and a blank one clears it. Nothing derived from the secret itself ever
     * goes over the wire.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    const val SECRET_MASK = "******"

    /**
     * Config and group key pattern
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private val KEY_REGEX = Regex("^[A-Za-z][A-Za-z0-9._-]*$")

    /**
     * Longest config key, kept in sync with `t_b_api_plugin_setting.setting_key`
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private const val MAX_KEY_LENGTH = 100

    /**
     * Every property a datasource declaration may carry
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private val DATASOURCE_PROPERTIES =
        setOf("name", "dbType", "required") + PluginDatasourceSlot.entries.map { it.name.lowercase() }

    /**
     * Mapper used to read the declared file; private so this object stays Spring-free
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private val mapper = JsonMapper.builder().build()

    /**
     * Parse a declared plugin config schema
     *
     * @param json Raw content of `META-INF/plugin-config.json`, or null / blank when
     *        the plugin ships none
     * @return Parsed schema, or null when nothing was declared
     * @throws IllegalArgumentException when the file cannot be honoured
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see PluginConfigSchema
     */
    fun parse(json: String?): PluginConfigSchema? {
        if (json.isNullOrBlank()) {
            return null
        }

        val root = try {
            mapper.readValue(json, Map::class.java)
        } catch (e: Exception) {
            throw IllegalArgumentException("Plugin config schema must be a JSON object: ${e.message}")
        } ?: throw IllegalArgumentException("Plugin config schema must be a JSON object")

        val version = intOf(root, "version", "Plugin config schema") ?: 1
        val fieldKeys = mutableSetOf<String>()
        val groupKeys = mutableSetOf<String>()

        val groups = when (val groupsNode = root["groups"]) {
            null -> emptyList()
            is List<*> -> groupsNode.mapIndexed { index, node ->
                parseGroup(node, index, groupKeys, fieldKeys)
            }

            else -> throw IllegalArgumentException("Plugin config schema 'groups' must be an array")
        }

        // Read after the groups rather than before: a datasource names the config fields
        // holding its connection, so those fields have to exist before a reference to one
        // can be checked - and checking it is the whole point of declaring slots by key
        val datasources = when (val datasourcesNode = root["datasources"]) {
            null -> emptyList()
            is List<*> -> parseDatasources(datasourcesNode, groups.flatMap { it.fields })
            else -> throw IllegalArgumentException("Plugin config schema 'datasources' must be an array")
        }

        return PluginConfigSchema(version = version, datasources = datasources, groups = groups)
    }

    /**
     * Check whether a submitted secret means "keep the stored one"
     *
     * @param value Submitted value of a secret field
     * @return true=the mask of the stored value was sent back untouched
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun isMasked(value: String): Boolean = value == SECRET_MASK

    /**
     * Validate submitted config values against a schema
     *
     * The stored values are passed in because a required field may already be satisfied by
     * an earlier save, and because a masked secret is a "keep" rather than a submitted
     * value - see [SECRET_MASK].
     *
     * Only the declared constraints of the fields themselves. A key that describes a
     * datasource is held to one more set of rules on top of these, because a field's
     * declared pattern is its author's convenience rather than a guarantee the gateway
     * can compose a connection from - see `PluginDatasourceUtil.resolve`, which the save
     * path and the mount both go through.
     *
     * @param schema Schema the values belong to
     * @param values Submitted values, keyed by config key
     * @param stored Keys that already hold a stored value
     * @throws IllegalArgumentException when a key is undeclared or a value is invalid
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see PluginConfigSchema
     */
    fun validate(schema: PluginConfigSchema, values: Map<String, String>, stored: Set<String> = emptySet()) {
        values.forEach { (key, value) ->
            val field = schema.fieldOf(key)
                ?: throw IllegalArgumentException("Plugin config key '$key' is not declared by the plugin")

            // Neither a mask nor a blank carries a value: the first keeps the stored one and
            // the second clears it, so there is nothing to check - validating the mask itself
            // would trip whatever length or pattern the field declares. Whether a cleared
            // secret is allowed at all is the required check's business, below
            if (field.type == PluginConfigFieldType.SECRET && (value.isEmpty() || isMasked(value))) {
                return@forEach
            }

            validateValue(field, value)
        }

        schema.fields.filter { it.required }.forEach { field ->
            val submitted = values[field.key]
            val satisfied = when {
                // A masked secret is only satisfied by a value that is actually stored
                field.type == PluginConfigFieldType.SECRET && submitted != null && isMasked(submitted) ->
                    stored.contains(field.key)

                // Anything else submitted has to carry a value, since a blank submission
                // clears the key rather than keeping it
                submitted != null -> submitted.isNotEmpty()

                // Nothing was submitted for this key, so the stored value or the declared
                // default the plugin would fall back to has to be there instead
                else -> stored.contains(field.key) || field.default != null
            }

            require(satisfied) { "Plugin config key '${field.key}' is required" }
        }
    }

    /**
     * Parse the datasource declarations
     *
     * @param nodes Declared nodes, in declaration order
     * @param fields Every field declared by the schema, which the slots point into
     * @return Datasource declarations
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see PluginConfigField
     * @see PluginDatasourceSchema
     */
    private fun parseDatasources(
        nodes: List<*>,
        fields: List<PluginConfigField>
    ): List<PluginDatasourceSchema> {
        val byKey = fields.associateBy { it.key }
        val names = mutableSetOf<String>()

        // Which slot already claimed a key: two connections composed from one value would
        // mean the administrator edits a field whose other reader they cannot see
        val claimed = mutableMapOf<String, String>()

        return nodes.mapIndexed { index, node ->
            parseDatasource(node, index, byKey, names, claimed)
        }
    }

    /**
     * Parse one datasource declaration
     *
     * @param node Declared node
     * @param index Position of the declaration, for error messages
     * @param fields Declared fields, by key
     * @param names Datasource names seen so far
     * @param claimed Config keys already claimed, by the slot that claimed them
     * @return Datasource declaration
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see PluginConfigField
     * @see PluginDatasourceSchema
     */
    private fun parseDatasource(
        node: Any?,
        index: Int,
        fields: Map<String, PluginConfigField>,
        names: MutableSet<String>,
        claimed: MutableMap<String, String>
    ): PluginDatasourceSchema {
        val what = "Plugin config schema datasource #$index"
        val map = node as? Map<*, *> ?: throw IllegalArgumentException("$what must be an object")

        val unknown = map.keys.filterNot { it is String && it in DATASOURCE_PROPERTIES }
        require(unknown.isEmpty()) { "$what declares properties it does not support: ${unknown.joinToString()}" }

        val name = stringOf(map, "name", what)
            ?: throw IllegalArgumentException("$what must declare a name")
        require(PluginDatasourceSchema.isValidName(name)) {
            "$what has name '$name', which must match ${PluginDatasourceSchema.NAME_REGEX.pattern}"
        }
        require(names.add(name)) { "Duplicate plugin config datasource name '$name'" }

        // The dialect is the one thing the administrator cannot be asked for, so a
        // declaration that leaves it out is rejected rather than defaulted
        val rawType = stringOf(map, "dbType", what)
            ?: throw IllegalArgumentException("$what must declare a dbType")
        val dbType = PluginDatasourceType.parse(rawType)
            ?: throw IllegalArgumentException(
                "$what has unknown dbType '$rawType' " +
                        "(expected one of ${PluginDatasourceType.entries.joinToString { it.name.lowercase() }})"
            )

        val slots = PluginDatasourceSlot.entries.mapNotNull { slot ->
            val slotName = slot.name.lowercase()
            val key = stringOf(map, slotName, what) ?: return@mapNotNull null
            val field = fields[key] ?: throw IllegalArgumentException(
                "$what declares a $slotName slot pointing at '$key', which is not a declared config field"
            )
            require(slotAccepts(slot, field.type)) {
                "$what declares a $slotName slot pointing at '$key', which is a ${field.type.name} field" +
                        " and cannot describe a ${slot.name}"
            }

            // One key, one connection fact: a field read as two slots would be edited by an
            // administrator who can see only one of the readers
            claimed.put(key, "$name.$slotName")?.let { previous ->
                throw IllegalArgumentException(
                    "Plugin config key '$key' is claimed by both $previous and $name.$slotName"
                )
            }

            // A required secret could never be cleared: a blank submission is refused for
            // being blank, so the stored password would be the administrator's only choice
            require(slot != PluginDatasourceSlot.PASSWORD || !field.required) {
                "$what declares a password slot pointing at '$key', which is required and could therefore" +
                        " never be cleared"
            }

            slot to key
        }.toMap()

        when (dbType) {
            // Without these two there is nothing to connect to, whatever else is declared
            PluginDatasourceType.MYSQL -> require(
                PluginDatasourceSlot.HOST in slots && PluginDatasourceSlot.DATABASE in slots
            ) {
                "$what is a MYSQL datasource and must declare both a host and a database slot"
            }

            // Nothing an administrator could decide about it, so nothing to point at
            PluginDatasourceType.SQLITE -> require(slots.isEmpty()) {
                "$what is a SQLITE datasource, which the gateway supplies itself and which declares no slot"
            }
        }

        return PluginDatasourceSchema(
            name = name,
            required = boolOf(map, "required", what) ?: false,
            dbType = dbType,
            slots = slots
        )
    }

    /**
     * Check whether a config field can describe a connection slot
     *
     * The rule that keeps a password a secret and every other slot plain: a secret field
     * is encrypted at rest and never returned by the API, so it may only be a password,
     * and a password that is not held as a secret would be stored in the clear.
     *
     * @param slot Connection fact
     * @param type Declared type of the field the slot points at
     * @return true=the field can hold this slot
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see PluginDatasourceSlot
     * @see PluginConfigFieldType
     */
    private fun slotAccepts(slot: PluginDatasourceSlot, type: PluginConfigFieldType): Boolean = when (slot) {
        PluginDatasourceSlot.PORT -> type == PluginConfigFieldType.NUMBER
        PluginDatasourceSlot.PASSWORD -> type == PluginConfigFieldType.SECRET

        PluginDatasourceSlot.HOST,
        PluginDatasourceSlot.DATABASE,
        PluginDatasourceSlot.USERNAME,
        PluginDatasourceSlot.PARAMS -> type == PluginConfigFieldType.STRING || type == PluginConfigFieldType.TEXT
    }

    /**
     * Parse one field group
     *
     * @param node Declared node
     * @param index Position of the group, for error messages
     * @param groupKeys Group keys seen so far
     * @param fieldKeys Field keys seen so far
     * @return Parsed group
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see PluginConfigFieldGroup
     */
    private fun parseGroup(
        node: Any?,
        index: Int,
        groupKeys: MutableSet<String>,
        fieldKeys: MutableSet<String>
    ): PluginConfigFieldGroup {
        val what = "Plugin config schema group #$index"
        val map = node as? Map<*, *> ?: throw IllegalArgumentException("$what must be an object")
        val key = keyOf(map, what, "group")
        require(groupKeys.add(key)) { "Duplicate plugin config group key '$key'" }

        val fieldsNode =
            map["fields"] ?: throw IllegalArgumentException("Plugin config group '$key' must declare fields")
        val fieldNodes = fieldsNode as? List<*>
            ?: throw IllegalArgumentException("Plugin config group '$key' fields must be an array")
        val fields = fieldNodes.mapIndexed { position, fieldNode ->
            parseField(fieldNode, key, position, fieldKeys)
        }
        require(fields.isNotEmpty()) { "Plugin config group '$key' must declare at least one field" }

        return PluginConfigFieldGroup(
            key = key,
            title = stringOf(map, "title", what),
            description = stringOf(map, "description", what),
            fields = fields
        )
    }

    /**
     * Parse one field
     *
     * @param node Declared node
     * @param groupKey Key of the group the field belongs to
     * @param index Position of the field inside its group, for error messages
     * @param fieldKeys Field keys seen so far
     * @return Parsed field
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see PluginConfigField
     */
    private fun parseField(
        node: Any?,
        groupKey: String,
        index: Int,
        fieldKeys: MutableSet<String>
    ): PluginConfigField {
        val what = "Plugin config field #$index of group '$groupKey'"
        val map = node as? Map<*, *> ?: throw IllegalArgumentException("$what must be an object")
        val key = keyOf(map, what, "field")
        require(fieldKeys.add(key)) { "Duplicate plugin config field key '$key'" }

        val rawType = stringOf(map, "type", what)
            ?: throw IllegalArgumentException("Plugin config field '$key' must declare a type")
        val type = PluginConfigFieldType.parse(rawType)
            ?: throw IllegalArgumentException(
                "Plugin config field '$key' has unknown type '$rawType' " +
                        "(expected one of ${PluginConfigFieldType.entries.joinToString { it.name.lowercase() }})"
            )

        val default = scalarOf(map, "default", what)
        val minimum = decimalOf(map, "minimum", what)
        val maximum = decimalOf(map, "maximum", what)
        val integer = boolOf(map, "integer", what) ?: false
        val minLength = intOf(map, "minLength", what)
        val maxLength = intOf(map, "maxLength", what)
        val pattern = stringOf(map, "pattern", what)
        val options = parseOptions(map["options"], key, type)

        require(!(type == PluginConfigFieldType.SECRET && default != null)) {
            "Plugin config field '$key' is a secret and must not declare a default"
        }
        require(type == PluginConfigFieldType.NUMBER || (minimum == null && maximum == null && !integer)) {
            "Plugin config field '$key' declares minimum / maximum / integer but is not a number"
        }
        val textConstraints = minLength != null || maxLength != null || pattern != null
        require(
            !textConstraints ||
                    type == PluginConfigFieldType.STRING ||
                    type == PluginConfigFieldType.TEXT ||
                    type == PluginConfigFieldType.SECRET
        ) {
            "Plugin config field '$key' declares minLength / maxLength / pattern but is not text"
        }
        pattern?.let {
            runCatching { Regex(it) }.getOrElse { error ->
                throw IllegalArgumentException("Plugin config field '$key' has an invalid pattern: ${error.message}")
            }
        }

        val field = PluginConfigField(
            key = key,
            type = type,
            title = stringOf(map, "title", what),
            description = stringOf(map, "description", what),
            default = default,
            required = boolOf(map, "required", what) ?: false,
            placeholder = stringOf(map, "placeholder", what),
            minimum = minimum,
            maximum = maximum,
            integer = integer,
            minLength = minLength,
            maxLength = maxLength,
            pattern = pattern,
            options = options
        )

        // The declared default has to satisfy the field's own constraints, otherwise the
        // plugin reads a value the administrator could never have entered by hand
        field.default?.let { validateValue(field, it) }

        return field
    }

    /**
     * Parse the options of an enum field
     *
     * @param node Declared node
     * @param key Key of the field the options belong to
     * @param type Type of the field the options belong to
     * @return Parsed options, or null when the field is not an enum
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see PluginConfigFieldType
     * @see PluginConfigFieldOption
     */
    private fun parseOptions(node: Any?, key: String, type: PluginConfigFieldType): List<PluginConfigFieldOption>? {
        if (type != PluginConfigFieldType.ENUM) {
            require(node == null) { "Plugin config field '$key' declares options but is not an enum" }

            return null
        }

        val nodes = node as? List<*>
            ?: throw IllegalArgumentException("Plugin config field '$key' must declare options")
        require(nodes.isNotEmpty()) { "Plugin config field '$key' must declare at least one option" }

        val options = nodes.mapIndexed { index, item ->
            val what = "Plugin config field '$key' option #$index"
            val map = item as? Map<*, *> ?: throw IllegalArgumentException("$what must be an object")
            val value = stringOf(map, "value", what) ?: throw IllegalArgumentException("$what must declare a value")

            PluginConfigFieldOption(value = value, label = stringOf(map, "label", what))
        }
        require(options.map { it.value }.toSet().size == options.size) {
            "Plugin config field '$key' declares duplicate option values"
        }

        return options
    }

    /**
     * Validate one value against its field
     *
     * @param field Field the value belongs to
     * @param value Submitted value
     * @throws IllegalArgumentException when the value is invalid
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see PluginConfigField
     */
    private fun validateValue(field: PluginConfigField, value: String) {
        val key = field.key

        when (field.type) {
            PluginConfigFieldType.BOOLEAN -> require(value.equals("true", true) || value.equals("false", true)) {
                "Plugin config key '$key' must be 'true' or 'false', got '$value'"
            }

            PluginConfigFieldType.NUMBER -> {
                val number = value.toBigDecimalOrNull()
                    ?: throw IllegalArgumentException("Plugin config key '$key' must be a number, got '$value'")

                if (field.integer) {
                    require(number.stripTrailingZeros().scale() <= 0) {
                        "Plugin config key '$key' must be a whole number, got '$value'"
                    }
                }
                field.minimum?.let {
                    require(number >= it) { "Plugin config key '$key' must be at least $it, got '$value'" }
                }
                field.maximum?.let {
                    require(number <= it) { "Plugin config key '$key' must be at most $it, got '$value'" }
                }
            }

            PluginConfigFieldType.ENUM -> {
                val allowed = field.options?.map { it.value } ?: emptyList()
                require(allowed.contains(value)) {
                    "Plugin config key '$key' must be one of ${allowed.joinToString()}, got '$value'"
                }
            }

            PluginConfigFieldType.STRING,
            PluginConfigFieldType.TEXT,
            PluginConfigFieldType.SECRET -> {
                field.minLength?.let {
                    require(value.length >= it) { "Plugin config key '$key' must be at least $it characters" }
                }
                field.maxLength?.let {
                    require(value.length <= it) { "Plugin config key '$key' must be at most $it characters" }
                }
                field.pattern?.let {
                    require(Regex(it).matches(value)) { "Plugin config key '$key' does not match $it" }
                }
            }
        }
    }

    /**
     * Read a config or group key
     *
     * @param map Declared object
     * @param what Description of the object, for error messages
     * @param kind What the key identifies, for error messages
     * @return Validated key
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private fun keyOf(map: Map<*, *>, what: String, kind: String): String {
        val key = stringOf(map, "key", what) ?: throw IllegalArgumentException("$what must declare a key")

        require(key.length <= MAX_KEY_LENGTH) {
            "Plugin config $kind key '$key' is longer than $MAX_KEY_LENGTH characters"
        }
        require(KEY_REGEX.matches(key)) {
            "Plugin config $kind key '$key' must match ${KEY_REGEX.pattern}"
        }

        return key
    }

    /**
     * Read a string property
     *
     * @param map Declared object
     * @param name Property name
     * @param what Description of the object, for error messages
     * @return Value, or null when absent
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private fun stringOf(map: Map<*, *>, name: String, what: String): String? =
        map[name]?.let { it as? String ?: throw IllegalArgumentException("$what '$name' must be a string") }

    /**
     * Read a boolean property
     *
     * @param map Declared object
     * @param name Property name
     * @param what Description of the object, for error messages
     * @return Value, or null when absent
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private fun boolOf(map: Map<*, *>, name: String, what: String): Boolean? =
        map[name]?.let { it as? Boolean ?: throw IllegalArgumentException("$what '$name' must be a boolean") }

    /**
     * Read an integer property
     *
     * @param map Declared object
     * @param name Property name
     * @param what Description of the object, for error messages
     * @return Value, or null when absent
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private fun intOf(map: Map<*, *>, name: String, what: String): Int? =
        map[name]?.let { it as? Number ?: throw IllegalArgumentException("$what '$name' must be a number") }
            ?.toInt()

    /**
     * Read a decimal property
     *
     * @param map Declared object
     * @param name Property name
     * @param what Description of the object, for error messages
     * @return Value, or null when absent
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private fun decimalOf(map: Map<*, *>, name: String, what: String): BigDecimal? =
        map[name]?.let { it as? Number ?: throw IllegalArgumentException("$what '$name' must be a number") }
            ?.let { BigDecimal(it.toString()) }

    /**
     * Read a property that may be declared as a string, a boolean or a number
     *
     * A schema author writing `"default": true` is stating the obvious, so the scalar
     * is accepted in its natural JSON shape and normalised to the string the setting
     * table stores. Integral numbers are normalised without a fractional part, so a
     * whole-number default is not rejected by an integer field's own check.
     *
     * @param map Declared object
     * @param name Property name
     * @param what Description of the object, for error messages
     * @return Normalised value, or null when absent
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private fun scalarOf(map: Map<*, *>, name: String, what: String): String? =
        map[name]?.let {
            when (it) {
                is String -> it
                is Boolean -> it.toString()
                is Number -> numberToString(it)
                else -> throw IllegalArgumentException("$what '$name' must be a string, a number or a boolean")
            }
        }

    /**
     * Normalise a declared number to the string form stored by the setting table
     *
     * @param number Declared number
     * @return Normalised value
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private fun numberToString(number: Number): String {
        if (number !is Double && number !is Float) {
            return number.toString()
        }

        val decimal = BigDecimal(number.toString()).stripTrailingZeros()

        return if (decimal.scale() <= 0) decimal.toBigInteger().toString() else decimal.toPlainString()
    }
}
