package top.fatweb.apimanagement.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import tools.jackson.databind.JsonNode
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import tools.jackson.module.kotlin.treeToValue
import top.fatweb.apimanagement.plugin.signing.PluginSigner
import java.io.File
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.UUID

/**
 * Uploads the signed plugin jar to a running gateway, making the release live.
 *
 * Authentication is the console's own access token: the author logs in, copies `accessToken`
 * out of the response and hands it to the task. The task therefore never calls `/login`,
 * never sees a credential and never persists the token - it only refuses to run without one.
 * The token is a shared, non-rotating secret, so it is wrapped as [Internal] rather than
 * [Input] to keep it out of the task's input fingerprint.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@UntrackedTask(because = "Uploads the plugin to a running gateway, an external side effect")
abstract class DeployPlugin : DefaultTask() {
    /**
     * Gateway base URL, e.g. `http://localhost:8080`.
     *
     * Optional at the Gradle level so that an absent value is reported by [run] with the
     * gradle property and environment variable named, rather than by Gradle's own
     * "property doesn't have a configured value" validation.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @get:Optional
    @get:Input
    abstract val serverUrl: Property<String>

    /**
     * Access token copied from a console login response. Required; never logged.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @get:Internal
    abstract val accessToken: Property<String>

    /**
     * Register the jar's signer public key with the gateway before installing.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @get:Input
    abstract val trustSigner: Property<Boolean>

    /**
     * The signed jar produced by `signPlugin`.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @get:Internal
    abstract val jarFile: RegularFileProperty

    /**
     * Directory holding dependency module jars produced by `generatePluginLock`.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @get:Internal
    abstract val modulesDirectory: DirectoryProperty

    // Held in the companion: task instances are serialized by the configuration cache, and a
    // Jackson mapper and an HttpClient are not part of a task's declared state
    private val mapper get() = MAPPER

    private val httpClient get() = HTTP_CLIENT

    @TaskAction
    fun run() {
        val baseUrl =
            serverUrl.orNull?.trim()?.takeIf { it.isNotEmpty() }
                ?: throw GradleException(
                    "apiPlugin.serverUrl is required. Set it in the apiPlugin { } block, pass " +
                        "-PapiPlugin.serverUrl=http://host:port, or export API_PLUGIN_SERVER_URL.",
                )
        val token =
            accessToken.orNull?.trim()?.takeIf { it.isNotEmpty() }
                ?: throw GradleException(
                    "apiPlugin.accessToken is required. Log into the console, copy accessToken from the " +
                        "login response, then pass -PapiPlugin.accessToken=... or export API_PLUGIN_ACCESS_TOKEN.",
                )

        val jar = jarFile.get().asFile
        if (!jar.isFile) {
            throw GradleException("Signed plugin jar not found at $jar - run ./gradlew signPlugin first.")
        }
        if (jar.length() > MAX_JAR_SIZE_BYTES) {
            throw GradleException(
                "Plugin jar is ${jar.length() / 1024 / 1024} MB; the gateway rejects uploads over " +
                    "${MAX_JAR_SIZE_BYTES / 1024 / 1024} MB.",
            )
        }

        val publicKeyPem =
            PluginSigner.readPublicKey(jar.toPath())
                ?: throw GradleException(
                    "No signer public key in ${jar.name} - the jar is unsigned. Run ./gradlew signPlugin first.",
                )
        val fingerprint = PluginSigner.spkiFingerprint(publicKeyPem)
        logger.lifecycle("Jar signer fingerprint: {}", fingerprint)

        val base = baseUrl.trimEnd('/')
        if (trustSigner.get()) {
            registerSigner(base, token, publicKeyPem, fingerprint)
        }
        uploadModules(base, token)

        val boundary = BOUNDARY_PREFIX + UUID.randomUUID().toString().replace("-", "")
        val install =
            HttpRequest
                .newBuilder(uri("$base/system/api/plugin/install"))
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "multipart/form-data; boundary=$boundary")
                .POST(multipartBody(boundary, jar.name, jar))
                .build()

        val reply = envelope(send(install), token)
        if (!reply.success) {
            gatewayFailure(reply, fingerprint)
        }

        val info = reply.data?.takeIf { !it.isNull() }?.let { data -> mapper.treeToValue<PluginInfo>(data) }
        logger.lifecycle(
            "Deployed plugin {} version {} (versionCode {})",
            info?.pluginId ?: jar.name,
            info?.versionName ?: "unknown",
            info?.versionCode ?: "unknown",
        )
    }

    /**
     * Register the signer public key, tolerating a key that is already trusted - the end
     * state the caller wants (the gateway trusts this signer) holds either way, and a
     * re-deploy must not fail on it.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private fun registerSigner(base: String, token: String, publicKeyPem: String, fingerprint: String) {
        val body = mapper.writeValueAsString(mapOf("publicKey" to publicKeyPem, "alias" to null))
        val request =
            HttpRequest
                .newBuilder(uri("$base/system/api/plugin/key"))
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build()

        val reply = envelope(send(request), token)
        when {
            reply.success -> logger.lifecycle("Registered the signer public key with the gateway: {}", fingerprint)
            reply.msg.orEmpty().contains("already exists", ignoreCase = true) ->
                logger.lifecycle("The gateway already trusts this signer: {}", fingerprint)

            else -> gatewayFailure(reply, fingerprint)
        }
    }

    /**
     * Uploads the dependency modules produced by `generatePluginLock`.
     *
     * @param base gateway base URL
     * @param token access token
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private fun uploadModules(base: String, token: String) {
        val directory = modulesDirectory.get().asFile
        if (!directory.isDirectory) {
            return
        }
        val modules =
            directory
                .listFiles { file -> file.isFile && file.name.endsWith(".jar") }
                ?.sortedBy { it.name }
                .orEmpty()
        modules.forEach { module ->
            val boundary = BOUNDARY_PREFIX + UUID.randomUUID().toString().replace("-", "")
            val request =
                HttpRequest
                    .newBuilder(uri("$base/system/api/plugin/module/install"))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Authorization", "Bearer $token")
                    .header("Content-Type", "multipart/form-data; boundary=$boundary")
                    .POST(multipartBody(boundary, module.name, module))
                    .build()
            val reply = envelope(send(request), token)
            if (!reply.success) {
                throw GradleException(
                    "Cannot install dependency module ${module.name}: " +
                        (reply.msg?.takeIf { it.isNotBlank() } ?: "gateway code ${reply.code}"),
                )
            }
            logger.lifecycle("Uploaded dependency module {}", module.name)
        }
    }

    private fun send(request: HttpRequest): HttpResponse<ByteArray> = try {
        httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray())
    } catch (_: InterruptedException) {
        Thread.currentThread().interrupt()
        throw GradleException("Interrupted while talking to the gateway at ${request.uri()}.")
    } catch (e: IOException) {
        throw GradleException(
            "Transport error talking to the gateway at ${request.uri()}: " +
                "${e.javaClass.simpleName}: ${e.message}. Is the gateway running and reachable?",
        )
    }

    /**
     * The gateway reports failures as HTTP 200 with `{code, success, msg, data}`, so the
     * status line alone says nothing - only the body does. The one exception is the
     * security filter, which can reject the token before the application runs.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private fun envelope(response: HttpResponse<ByteArray>, token: String): GatewayReply {
        val status = response.statusCode()
        val body = response.body().toString(Charsets.UTF_8)
        if (status == 401 || status == 403) {
            tokenRejected(status, null, token)
        }

        val reply =
            try {
                mapper.readValue<GatewayReply>(body)
            } catch (_: Exception) {
                throw GradleException(
                    "The gateway answered HTTP $status but not with a response envelope: ${body.take(300)}",
                )
            }
        if (reply.code in AUTH_FAILURE_CODES) {
            tokenRejected(status, reply.msg, token)
        }

        return reply
    }

    private fun tokenRejected(status: Int, msg: String?, token: String): Nothing {
        val detail = msg?.takeIf { it.isNotBlank() }?.let { " ($it)" }.orEmpty()
        throw GradleException(
            "The gateway rejected the access token (HTTP $status$detail).\n" +
                "Log into the console, copy a fresh accessToken from the login response, and pass it as " +
                "-PapiPlugin.accessToken=... or API_PLUGIN_ACCESS_TOKEN.\n" +
                "The token supplied ${describeExpiry(token)}; if that time is still ahead, the gateway's " +
                "Redis no longer holds the session, so a fresh login is needed either way.",
        )
    }

    /**
     * Decode the `exp` claim of the supplied token so the author can tell "it expired" from
     * "the gateway's Redis lost it". Only the payload segment is base64url-decoded - no JWT
     * library, and the token itself is never printed.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private fun describeExpiry(token: String): String {
        val segment =
            token.split('.').getOrNull(1)
                ?: return "is not a JWT (it has no payload segment)"
        val payload =
            try {
                String(Base64.getUrlDecoder().decode(segment), Charsets.UTF_8)
            } catch (_: IllegalArgumentException) {
                return "has a payload that is not base64url"
            }
        val exp =
            try {
                mapper.readTree(payload).get("exp")?.asLong()
            } catch (_: Exception) {
                null
            } ?: return "carries no decodable 'exp' claim"

        return "expired at ${Instant.ofEpochSecond(exp)} (epoch $exp)"
    }

    private fun gatewayFailure(reply: GatewayReply, fingerprint: String): Nothing {
        val msg = reply.msg?.takeIf { it.isNotBlank() } ?: "the gateway reported failure (code ${reply.code})"
        val hint =
            when (reply.code) {
                CODE_PLUGIN_NOT_TRUSTED ->
                    "The gateway does not trust the jar's signer (fingerprint $fingerprint). Add the key in " +
                        "the console, or re-run with -PapiPlugin.trustSigner=true to register it from here."

                CODE_PLUGIN_SIGNATURE_INVALID -> "The jar's signature did not verify - run ./gradlew signPlugin."

                CODE_PLUGIN_VERSION_CONFLICT ->
                    "The version being deployed is older than the one already installed - bump " +
                        "apiPlugin.versionCode above it."

                else -> null
            }

        throw GradleException(if (hint == null) msg else "$msg\n$hint")
    }

    private fun uri(url: String): URI = try {
        URI.create(url)
    } catch (_: IllegalArgumentException) {
        throw GradleException("apiPlugin.serverUrl is not a valid URL: $url")
    }

    private fun multipartBody(boundary: String, fileName: String, file: File): HttpRequest.BodyPublisher {
        val escaped = fileName.replace("\\", "\\\\").replace("\"", "\\\"")
        val header =
            buildString {
                append("--").append(boundary).append(CRLF)
                append("Content-Disposition: form-data; name=\"file\"; filename=\"")
                    .append(escaped)
                    .append("\"")
                    .append(CRLF)
                append("Content-Type: application/java-archive").append(CRLF).append(CRLF)
            }.toByteArray(Charsets.UTF_8)
        val footer = "$CRLF--$boundary--$CRLF".toByteArray(Charsets.UTF_8)

        return HttpRequest.BodyPublishers.concat(
            HttpRequest.BodyPublishers.ofByteArray(header),
            HttpRequest.BodyPublishers.ofFile(file.toPath()),
            HttpRequest.BodyPublishers.ofByteArray(footer),
        )
    }

    /**
     * Response envelope shared by every gateway endpoint: `{code, success, msg, data}`.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    data class GatewayReply(
        val code: Int = 0,
        val success: Boolean = false,
        val msg: String? = null,
        val data: JsonNode? = null,
    )

    /**
     * The `data` of a successful install: the identity of the plugin that is now live.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    data class PluginInfo(val pluginId: String? = null, val versionName: String? = null, val versionCode: Int? = null)

    companion object {
        // Matches the gateway's default cap (app.storage.plugin-jar-max-bytes); refusing here
        // saves uploading something the gateway is certain to reject.
        private const val MAX_JAR_SIZE_BYTES = 200L * 1024 * 1024

        // Codes the gateway returns in the envelope (ResponseCode).
        private const val CODE_PLUGIN_NOT_TRUSTED = 40064
        private const val CODE_PLUGIN_SIGNATURE_INVALID = 40065
        private const val CODE_PLUGIN_VERSION_CONFLICT = 40066

        // PERMISSION_UNAUTHORIZED / PERMISSION_TOKEN_ILLEGAL / PERMISSION_TOKEN_HAS_EXPIRED -
        // the token itself is the problem, as opposed to ACCESS_DENIED (20052), which is a
        // token the gateway accepted from an account that lacks the authority.
        private val AUTH_FAILURE_CODES = setOf(20050, 20060, 20061)

        private val CONNECT_TIMEOUT: Duration = Duration.ofSeconds(30)
        private val REQUEST_TIMEOUT: Duration = Duration.ofMinutes(5)

        private val MAPPER = jacksonObjectMapper()
        private val HTTP_CLIENT: HttpClient =
            HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build()

        private const val BOUNDARY_PREFIX = "----apiPluginBoundary"
        private const val CRLF = "\r\n"
    }
}
