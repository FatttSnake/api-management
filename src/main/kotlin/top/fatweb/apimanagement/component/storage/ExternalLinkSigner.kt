package top.fatweb.apimanagement.component.storage

import org.springframework.stereotype.Component
import top.fatweb.apimanagement.properties.ServerProperties
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * External link signer
 *
 * Mints and verifies the stateless bearer credential carried by a login-free download
 * URL: nothing is stored server-side, so a link cannot be revoked before it expires.
 * The signed payload is `"plugin-storage-v1\n{key}\n{expiresAt}"`.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Component
class ExternalLinkSigner(
    private val serverProperties: ServerProperties
) {
    private companion object {
        const val PURPOSE = "plugin-storage-v1"
        const val ALGORITHM = "HmacSHA256"
    }

    private fun canonical(key: String, expiresAt: Long): String =
        "$PURPOSE\n$key\n$expiresAt"

    private fun digest(key: String, expiresAt: Long): ByteArray {
        val mac = Mac.getInstance(ALGORITHM)
        mac.init(SecretKeySpec(serverProperties.security.tokenSecret.toByteArray(Charsets.UTF_8), ALGORITHM))

        return mac.doFinal(canonical(key, expiresAt).toByteArray(Charsets.UTF_8))
    }

    /**
     * Sign a location key and its expiry
     *
     * @param key Location key
     * @param expiresAt Expiry as epoch seconds
     * @return URL-safe base64 signature without padding
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun sign(key: String, expiresAt: Long): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(digest(key, expiresAt))

    /**
     * Verify a signature produced by [sign]
     *
     * The comparison is constant time.
     *
     * @param key Location key
     * @param expiresAt Expiry as epoch seconds
     * @param signature Presented signature
     * @return true=valid; false=invalid or malformed
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun verify(key: String, expiresAt: Long, signature: String): Boolean {
        val presented = runCatching {
            Base64.getUrlDecoder().decode(signature)
        }.getOrNull() ?: return false

        return MessageDigest.isEqual(digest(key, expiresAt), presented)
    }
}
