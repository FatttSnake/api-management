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
 * Mints and verifies the stateless bearer credential carried by a login-free
 * download URL. Nothing is stored server-side, so the same link verifies on every
 * instance without shared state; the price is that a link cannot be revoked before
 * it expires.
 *
 * The signed payload is `"plugin-storage-v1\n{key}\n{expiresAt}"`. The purpose
 * prefix keeps this scheme domain-separated from the JWT that the same secret
 * signs, and the path segment charset forbids control characters, so no key can
 * contain a newline and shift bytes across a field boundary.
 *
 * Only the object identity and the expiry are signed — never the scheme, host or
 * other query parameters — so the same link keeps working whether it is reached
 * through a proxy or directly, and a proxy appending its own query parameter
 * cannot break verification.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ServerProperties
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
     * The comparison is constant time. It runs over the decoded bytes rather than
     * the encoded strings, because a string comparison short-circuits on the first
     * differing character.
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
