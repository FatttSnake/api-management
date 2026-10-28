package top.fatweb.apimanagement.api.v1

/**
 * The only gateway surface a plugin can reach.
 *
 * The context exposes capabilities, never the gateway's Spring container, beans, mappers or
 * object mapper. Every capability is scoped to the calling plugin.
 *
 * The accessors are functions rather than properties so that Java plugin authors write
 * `context.api()` instead of `context.getApi()`.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
interface GatewayContext {
    /**
     * Route registration, valid during [ApiPlugin.onStart].
     *
     * @return route registry
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun api(): ApiRoutes

    /**
     * Plugin-scoped settings.
     *
     * @return settings capability
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun settings(): Settings

    /**
     * Plugin-scoped datasources.
     *
     * @return datasource capability
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun dataSources(): DataSources

    /**
     * Plugin-scoped file and content storage.
     *
     * @return storage capability
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun storage(): Storage

    /**
     * Read-only platform user data.
     *
     * @return user capability
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun users(): Users

    /**
     * Audit sink.
     *
     * @return audit capability
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun audit(): AuditSink

    /**
     * In-process event bus.
     *
     * @return event capability
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun events(): EventBus

    /**
     * Outbound HTTP client.
     *
     * @return HTTP capability
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun http(): HttpCapability

    /**
     * Scheduler owned by the gateway.
     *
     * @return scheduler capability
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun scheduler(): Scheduler

    /**
     * Gateway JSON codec.
     *
     * @return JSON capability
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun json(): JsonCodec

    /**
     * Plugin logger with plugin and trace context.
     *
     * @return logger capability
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun logger(): PluginLogger

    /**
     * Runtime metadata.
     *
     * @return metadata capability
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun metadata(): PluginMetadata
}
