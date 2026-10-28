package top.fatweb.apimanagement.api.v1

import java.math.BigDecimal

/**
 * Runtime governance snapshot of a registered interface.
 *
 * @property code interface code, for example `api:echo:v1:ping`
 * @property pluginId owning plugin
 * @property apiVersion declared API version
 * @property name display name
 * @property enabled whether calls are admitted
 * @property price price per call, or null when free or inherited
 * @property billingMode billing mode
 * @property needKey whether an AccessKey is required
 * @property rateLimit per-minute rate limit, or null when inherited or unlimited
 * @property accessMode access mode
 * @property path full path
 * @property method HTTP method
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
data class InterfaceInfo(
    val code: String,
    val pluginId: String,
    val apiVersion: Int,
    val name: String = "",
    val enabled: Boolean = true,
    val price: BigDecimal? = null,
    val billingMode: String = "",
    val needKey: Boolean = false,
    val rateLimit: Int? = null,
    val accessMode: String = "",
    val path: String = "",
    val method: String = ""
) {
    init {
        require(code.isNotBlank()) { "code must not be blank" }
        require(pluginId.isNotBlank()) { "pluginId must not be blank" }
    }
}
