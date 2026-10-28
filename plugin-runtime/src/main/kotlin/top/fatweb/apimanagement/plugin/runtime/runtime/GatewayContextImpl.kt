package top.fatweb.apimanagement.plugin.runtime.runtime

import top.fatweb.apimanagement.api.v1.ApiRoutes
import top.fatweb.apimanagement.api.v1.AuditSink
import top.fatweb.apimanagement.api.v1.DataSources
import top.fatweb.apimanagement.api.v1.EventBus
import top.fatweb.apimanagement.api.v1.GatewayContext
import top.fatweb.apimanagement.api.v1.HttpCapability
import top.fatweb.apimanagement.api.v1.JsonCodec
import top.fatweb.apimanagement.api.v1.PluginLogger
import top.fatweb.apimanagement.api.v1.PluginMetadata
import top.fatweb.apimanagement.api.v1.Scheduler
import top.fatweb.apimanagement.api.v1.Settings
import top.fatweb.apimanagement.api.v1.Storage
import top.fatweb.apimanagement.api.v1.Users

/**
 * [GatewayContext] backed by a staged route collector and plugin-scoped services.
 *
 * @param descriptor plugin descriptor
 * @param routes staged route collector
 * @param services plugin-scoped capabilities
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
class GatewayContextImpl(
    private val descriptor: PluginDescriptor,
    private val routes: ApiRoutes,
    private val services: PluginRuntimeServices
) : GatewayContext {
    private val pluginLogger: PluginLogger = services.loggerFactory(descriptor.pluginId)

    /**
     * Route registration.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    override fun api(): ApiRoutes = routes

    /**
     * Plugin settings.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    override fun settings(): Settings = services.settings

    /**
     * Plugin datasources.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    override fun dataSources(): DataSources = services.dataSources

    /**
     * Plugin storage.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    override fun storage(): Storage = services.storage

    /**
     * Read-only user data.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    override fun users(): Users = services.users

    /**
     * Audit sink.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    override fun audit(): AuditSink = services.audit

    /**
     * Event bus.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    override fun events(): EventBus = services.events

    /**
     * Outbound HTTP.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    override fun http(): HttpCapability = services.http

    /**
     * Scheduler.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    override fun scheduler(): Scheduler = services.scheduler

    /**
     * JSON codec.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    override fun json(): JsonCodec = services.json

    /**
     * Plugin logger.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    override fun logger(): PluginLogger = pluginLogger

    /**
     * Runtime metadata.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    override fun metadata(): PluginMetadata = PluginMetadata(
        pluginId = descriptor.pluginId,
        pluginVersion = descriptor.versionName,
        gatewayVersion = services.gatewayVersion,
        apiVersion = descriptor.apiVersion,
        gatewayApiVersion = services.gatewayApiVersion
    )
}
