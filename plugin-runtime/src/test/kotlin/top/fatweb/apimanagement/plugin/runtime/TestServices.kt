package top.fatweb.apimanagement.plugin.runtime

import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import top.fatweb.apimanagement.api.v1.AuditEvent
import top.fatweb.apimanagement.api.v1.AuditSink
import top.fatweb.apimanagement.api.v1.DataSources
import top.fatweb.apimanagement.api.v1.EventBus
import top.fatweb.apimanagement.api.v1.HttpCapability
import top.fatweb.apimanagement.api.v1.JsonCodec
import top.fatweb.apimanagement.api.v1.PluginLogger
import top.fatweb.apimanagement.api.v1.Scheduler
import top.fatweb.apimanagement.api.v1.Settings
import top.fatweb.apimanagement.api.v1.Storage
import top.fatweb.apimanagement.api.v1.Subscription
import top.fatweb.apimanagement.api.v1.UserInfo
import top.fatweb.apimanagement.api.v1.Users
import top.fatweb.apimanagement.plugin.runtime.module.FileSystemPluginModuleStore
import top.fatweb.apimanagement.plugin.runtime.runtime.PluginRuntimeServices
import java.math.BigDecimal
import java.time.Duration
import javax.sql.DataSource

/**
 * In-memory capabilities for runtime tests.
 *
 * @return services
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
fun testServices(): PluginRuntimeServices {
    val settings = object : Settings {
        private val values = linkedMapOf<String, String>()

        override fun get(key: String): String? = values[key]

        override fun getSecret(key: String): String? = values[key]

        override fun save(key: String, value: String) {
            values[key] = value
        }

        override fun all(): Map<String, String> = values.toMap()
    }

    val dataSources = object : DataSources {
        override fun names(): Set<String> = emptySet()

        override fun all(): Map<String, DataSource> = emptyMap()

        override fun get(name: String): DataSource? = null
    }

    val storage = object : Storage {
        private val contents = linkedMapOf<String, ByteArray>()
        private val files = linkedMapOf<String, ByteArray>()

        override fun saveContent(content: ByteArray): String {
            val key = FileSystemPluginModuleStore.sha256(content)
            contents[key] = content
            return key
        }

        override fun loadContent(key: String): ByteArray? = contents[key]

        override fun existsContent(key: String): Boolean = contents.containsKey(key)

        override fun deleteContent(key: String) {
            contents.remove(key)
        }

        override fun saveFile(path: String, content: ByteArray) {
            files[path] = content
        }

        override fun loadFile(path: String): ByteArray? = files[path]

        override fun existsFile(path: String): Boolean = files.containsKey(path)

        override fun deleteFile(path: String) {
            files.remove(path)
        }

        override fun fileExternalUrl(path: String, ttl: Duration?): String? = null
    }

    val users = object : Users {
        override fun findUser(userId: Long): UserInfo = UserInfo(userId)

        override fun getBalance(userId: Long): BigDecimal = BigDecimal.ZERO
    }

    val audit = object : AuditSink {
        override fun record(event: AuditEvent) {
        }
    }

    val events = object : EventBus {
        override fun publish(topic: String, payload: ByteArray) {
        }

        override fun subscribe(topic: String, subscriber: EventBus.EventSubscriber): Subscription =
            Subscription { }
    }

    val http = object : HttpCapability {
        override fun execute(request: HttpCapability.HttpRequest): HttpCapability.HttpResponse =
            throw UnsupportedOperationException("HTTP is not available in this test")
    }

    val scheduler = object : Scheduler {
        override fun scheduleWithFixedDelay(
            initialDelay: Duration,
            delay: Duration,
            task: Runnable
        ): Subscription = Subscription { }

        override fun scheduleCron(expression: String, task: Runnable): Subscription = Subscription { }
    }

    val mapper = JsonMapper.builder().addModules(KotlinModule.Builder().build()).build()
    val json = object : JsonCodec {
        override fun encode(value: Any?): ByteArray = mapper.writeValueAsBytes(value)

        override fun <T> decode(json: ByteArray, type: Class<T>): T = mapper.readValue(json, type)

        override fun <T> decode(json: String, type: Class<T>): T = mapper.readValue(json, type)
    }

    val logger = object : PluginLogger {
        override fun debug(format: String, vararg args: Any?) {
        }

        override fun info(format: String, vararg args: Any?) {
        }

        override fun warn(format: String, vararg args: Any?) {
        }

        override fun error(format: String, vararg args: Any?) {
        }

        override fun error(format: String, cause: Throwable, vararg args: Any?) {
        }
    }

    return PluginRuntimeServices(
        settings = settings,
        dataSources = dataSources,
        storage = storage,
        users = users,
        audit = audit,
        events = events,
        http = http,
        scheduler = scheduler,
        json = json,
        loggerFactory = { logger },
        gatewayVersion = "1.0.0",
        gatewayApiVersion = "1.0.0"
    )
}
