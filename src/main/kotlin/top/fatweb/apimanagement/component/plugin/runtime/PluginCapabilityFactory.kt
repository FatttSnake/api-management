package top.fatweb.apimanagement.component.plugin.runtime

import com.baomidou.mybatisplus.extension.kotlin.KtQueryWrapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.info.BuildProperties
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import top.fatweb.apimanagement.api.v1.AuditEvent
import top.fatweb.apimanagement.api.v1.AuditSink
import top.fatweb.apimanagement.api.v1.DataSources
import top.fatweb.apimanagement.api.v1.JsonCodec
import top.fatweb.apimanagement.api.v1.PluginLogger
import top.fatweb.apimanagement.api.v1.Settings
import top.fatweb.apimanagement.api.v1.Users
import top.fatweb.apimanagement.component.plugin.PluginConfigSchema
import top.fatweb.apimanagement.component.plugin.PluginConfigSchemaCache
import top.fatweb.apimanagement.component.plugin.PluginConfigSchemaUtil
import top.fatweb.apimanagement.component.plugin.PluginDatasourceFactory
import top.fatweb.apimanagement.component.plugin.PluginSettingReader
import top.fatweb.apimanagement.component.plugin.PluginStorageFactory
import top.fatweb.apimanagement.entity.system.EventLog
import top.fatweb.apimanagement.mapper.permission.UserInfoMapper
import top.fatweb.apimanagement.mapper.permission.UserMapper
import top.fatweb.apimanagement.mapper.system.EventLogMapper
import top.fatweb.apimanagement.plugin.runtime.runtime.GatewayApiInfo
import top.fatweb.apimanagement.plugin.runtime.runtime.PluginRuntimeServices
import top.fatweb.apimanagement.service.api.IApiAccountService
import top.fatweb.apimanagement.service.api.IApiPluginSettingService
import java.time.LocalDateTime
import javax.sql.DataSource
import top.fatweb.apimanagement.api.v1.UserInfo as ApiUserInfo
import top.fatweb.apimanagement.entity.permission.UserInfo as UserInfoEntity

/**
 * Builds the gateway capabilities handed to one plugin.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Component
class PluginCapabilityFactory(
    private val pluginSettingReader: PluginSettingReader,
    private val pluginConfigSchemaCache: PluginConfigSchemaCache,
    private val apiPluginSettingService: IApiPluginSettingService,
    private val pluginDatasourceFactory: PluginDatasourceFactory,
    private val pluginStorageFactory: PluginStorageFactory,
    private val apiAccountService: IApiAccountService,
    private val userMapper: UserMapper,
    private val userInfoMapper: UserInfoMapper,
    private val eventLogMapper: EventLogMapper,
    private val jsonMapper: JsonMapper,
    private val eventBus: PluginEventBus,
    private val scheduler: PluginScheduler,
    private val httpClient: PluginHttpClient,
    private val buildProperties: ObjectProvider<BuildProperties>,
) {
    /**
     * Creates the capabilities of a plugin.
     *
     * @param pluginId plugin ID
     * @param schema schema the plugin declares, read from the cache when omitted
     * @return runtime services
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun create(
        pluginId: String,
        schema: PluginConfigSchema? = pluginConfigSchemaCache.get(pluginId),
    ): PluginRuntimeServices {
        val datasources = pluginDatasourceFactory.create(pluginId, schema)

        return PluginRuntimeServices(
            settings = settings(pluginId, schema),
            dataSources = dataSources(datasources),
            storage = pluginStorageFactory.create(pluginId),
            users = users(),
            audit = audit(pluginId),
            events = eventBus.scoped(pluginId),
            http = httpClient.capability(),
            scheduler = scheduler.scoped(pluginId),
            json = json(),
            loggerFactory = { id -> Slf4jPluginLogger(LoggerFactory.getLogger("plugin.$id")) },
            gatewayVersion = buildProperties.getIfAvailable()?.version ?: "1.0.0",
            gatewayApiVersion = GatewayApiInfo.version,
            close = { datasources.values.forEach { (it as? AutoCloseable)?.close() } },
        )
    }

    private fun settings(pluginId: String, schema: PluginConfigSchema?): Settings = object : Settings {
        override fun get(key: String): String? = pluginSettingReader.resolveRaw(pluginId, key)

        override fun getSecret(key: String): String? = pluginSettingReader.resolve(pluginId, key)

        override fun save(key: String, value: String) {
            require(schema?.isDeclared(key) != true) {
                "Setting '$key' is declared by the plugin config schema and can only be changed by an administrator"
            }
            require(key.length <= PluginConfigSchemaUtil.MAX_KEY_LENGTH) {
                "Setting key '$key' is longer than ${PluginConfigSchemaUtil.MAX_KEY_LENGTH} characters"
            }
            require(value.toByteArray().size <= PluginConfigSchemaUtil.MAX_VALUE_BYTES) {
                "Setting value is longer than ${PluginConfigSchemaUtil.MAX_VALUE_BYTES} bytes"
            }
            apiPluginSettingService.set(pluginId, key, value)
        }

        override fun all(): Map<String, String> {
            val keys = apiPluginSettingService.listByPlugin(pluginId).keys
            return keys
                .filterNot { schema?.isSecret(it) == true }
                .mapNotNull { key -> pluginSettingReader.resolveRaw(pluginId, key)?.let { key to it } }
                .toMap()
        }
    }

    private fun dataSources(sources: Map<String, DataSource>): DataSources = object : DataSources {
        override fun names(): Set<String> = sources.keys

        override fun all(): Map<String, DataSource> = sources

        override fun get(name: String): DataSource? = sources[name]
    }

    private fun users(): Users = object : Users {
        override fun findUser(userId: Long): ApiUserInfo? {
            val user = userMapper.selectById(userId) ?: return null
            val info =
                userInfoMapper.selectOne(
                    KtQueryWrapper(UserInfoEntity()).eq(UserInfoEntity::userId, userId),
                )
            return ApiUserInfo(
                id = user.id ?: userId,
                username = user.username.orEmpty(),
                nickname = info?.nickname.orEmpty(),
                email = info?.email.orEmpty(),
                enabled = user.enable == 1,
            )
        }

        override fun getBalance(userId: Long) = apiAccountService.getBalance(userId)
    }

    private fun audit(pluginId: String): AuditSink = object : AuditSink {
        override fun record(event: AuditEvent) {
            eventLogMapper.insert(
                EventLog().apply {
                    this.event = EventLog.Event.API
                    operateTime = LocalDateTime.now()
                    detail = "[$pluginId] ${event.action} ${event.target} ${event.detail} ${event.attributes}"
                },
            )
        }
    }

    private fun json(): JsonCodec = object : JsonCodec {
        override fun encode(value: Any?): ByteArray = jsonMapper.writeValueAsBytes(value)

        override fun <T> decode(json: ByteArray, type: Class<T>): T = jsonMapper.readValue(json, type)

        override fun <T> decode(json: String, type: Class<T>): T = jsonMapper.readValue(json, type)
    }

    private class Slf4jPluginLogger(private val logger: org.slf4j.Logger) : PluginLogger {
        override fun debug(format: String, vararg args: Any?) = logger.debug(format, *args)

        override fun info(format: String, vararg args: Any?) = logger.info(format, *args)

        override fun warn(format: String, vararg args: Any?) = logger.warn(format, *args)

        override fun error(format: String, vararg args: Any?) = logger.error(format, *args)

        override fun error(format: String, cause: Throwable, vararg args: Any?) {
            val all = arrayOfNulls<Any?>(args.size + 1)
            System.arraycopy(args, 0, all, 0, args.size)
            all[args.size] = cause
            logger.error(format, *all)
        }
    }
}
