package top.fatweb.apimanagement.plugin.runtime

import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

/**
 * JSON mapper shared by the descriptor and dependency-lock parsers.
 *
 * A plugin descriptor may gain fields the running gateway does not know yet; refusing the
 * whole plugin over one of them would make adding a field a breaking change.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
internal val PLUGIN_JSON_MAPPER: JsonMapper =
    JsonMapper
        .builder()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .addModules(KotlinModule.Builder().build())
        .build()
