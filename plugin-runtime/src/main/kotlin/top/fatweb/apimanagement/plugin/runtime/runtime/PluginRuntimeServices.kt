package top.fatweb.apimanagement.plugin.runtime.runtime

import top.fatweb.apimanagement.api.v1.AuditSink
import top.fatweb.apimanagement.api.v1.DataSources
import top.fatweb.apimanagement.api.v1.EventBus
import top.fatweb.apimanagement.api.v1.HttpCapability
import top.fatweb.apimanagement.api.v1.JsonCodec
import top.fatweb.apimanagement.api.v1.PluginLogger
import top.fatweb.apimanagement.api.v1.Scheduler
import top.fatweb.apimanagement.api.v1.Settings
import top.fatweb.apimanagement.api.v1.Storage
import top.fatweb.apimanagement.api.v1.Users

/**
 * Gateway capabilities supplied to one plugin runtime.
 *
 * The implementations are plugin-scoped; the loader does not know where they come from.
 *
 * @property settings plugin settings
 * @property dataSources plugin datasources
 * @property storage plugin storage
 * @property users read-only user data
 * @property audit audit sink
 * @property events event bus
 * @property http outbound HTTP
 * @property scheduler scheduler
 * @property json JSON codec
 * @property loggerFactory creates a logger for a plugin ID
 * @property gatewayVersion gateway version
 * @property gatewayApiVersion gateway API version
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
class PluginRuntimeServices(
    val settings: Settings,
    val dataSources: DataSources,
    val storage: Storage,
    val users: Users,
    val audit: AuditSink,
    val events: EventBus,
    val http: HttpCapability,
    val scheduler: Scheduler,
    val json: JsonCodec,
    val loggerFactory: (String) -> PluginLogger,
    val gatewayVersion: String,
    val gatewayApiVersion: String
)
