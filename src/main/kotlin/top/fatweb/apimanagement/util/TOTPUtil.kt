package top.fatweb.apimanagement.util

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import org.apache.commons.codec.binary.Base32
import java.net.URLEncoder
import java.nio.ByteBuffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.and
import kotlin.io.encoding.Base64
import kotlin.math.pow
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

/**
 * TOTP util
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
object TOTPUtil {
    private val TIME_PERIOD = 30.seconds
    private val FLEXIBLE_TIME = 5.seconds
    private const val DIGITS = 6

    private fun computeCounterForMillis(millis: Long): Long = Math.floorDiv(millis, TIME_PERIOD.inWholeMilliseconds)

    private fun generateHash(secret: ByteArray, payload: ByteArray): ByteArray {
        val secretKey = Base32().decode(secret)
        val hmacSha1 = Mac.getInstance("HmacSHA1")
        hmacSha1.init(SecretKeySpec(secretKey, "RAW"))

        return hmacSha1.doFinal(payload)
    }

    private fun truncateHash(hash: ByteArray): ByteArray {
        val offset = hash.last().and(0x0F).toInt()
        val truncatedHash = ByteArray(4)
        for (i in 0..3) {
            truncatedHash[i] = hash[offset + i]
        }
        truncatedHash[0] = truncatedHash[0].and(0x7F)

        return truncatedHash
    }

    private fun calculateCode(key: String, time: Long): String {
        val timeCounter = computeCounterForMillis(time)
        val payload = ByteBuffer.allocate(8).putLong(0, timeCounter).array()
        val secretKey = key.toByteArray(Charsets.UTF_8)
        val hash = generateHash(secretKey, payload)
        val truncatedHash = truncateHash(hash)
        val code = ByteBuffer.wrap(truncatedHash).int % 10.0.pow(DIGITS).toInt()

        return code.toString().padStart(DIGITS, '0')
    }

    /**
     * Generate TOTP code
     *
     * @param secretKey Secret key
     * @return TOTP Code
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun generateCode(secretKey: String, time: Long = System.currentTimeMillis()): String =
        calculateCode(secretKey, time)

    /**
     * Validate TOTP code
     *
     * @param secretKey Secret key
     * @param code TOTP code
     * @return Result
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun validateCode(secretKey: String, code: String): Boolean {
        var time = System.currentTimeMillis()
        if (calculateCode(secretKey, time) == code) {
            return true
        }
        time -= FLEXIBLE_TIME.inWholeMilliseconds

        return calculateCode(secretKey, time) == code
    }

    private fun encodeUrl(str: String) = URLEncoder.encode(str, Charsets.UTF_8).replace("+", "%20")

    /**
     * Generate secret key
     *
     * @param length Secret key length
     * @return Secret key
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun generateSecretKey(length: Int = 16): String {
        if (length < 3) {
            throw IllegalArgumentException("Password length should be at least 3.")
        }

        val uppercaseChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val digits = "234567"
        val allChars = uppercaseChars + digits

        val secretKey = StringBuilder()
        secretKey.append(
            uppercaseChars[Random.nextInt(uppercaseChars.length)],
            digits[Random.nextInt(digits.length)]
        )
        repeat(length - 3) {
            secretKey.append(allChars[Random.nextInt(allChars.length)])
        }

        return secretKey.toString().toList().shuffled().joinToString("")
    }

    /**
     * Generate TOTP URL
     *
     * @param issuer Issuer
     * @param username Username
     * @param secretKey Secret key
     * @return TOTP URL
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun generateUrl(issuer: String, username: String, secretKey: String): String =
        "otpauth://totp/${encodeUrl(issuer)}:${encodeUrl(username)}?secret=${encodeUrl(secretKey)}"

    /**
     * Generate TOTP QR code
     *
     * @param issuer Issuer
     * @param username Username
     * @param secretKey Secret key
     * @return QR code
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see BitMatrix
     */
    fun generateQRCode(issuer: String, username: String, secretKey: String): BitMatrix {
        val hints = HashMap<EncodeHintType, Any>()
        hints[EncodeHintType.CHARACTER_SET] = Charsets.UTF_8
        hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.H
        hints[EncodeHintType.MARGIN] = 1

        return QRCodeWriter().encode(generateUrl(issuer, username, secretKey), BarcodeFormat.QR_CODE, 200, 200, hints)
    }

    /**
     * Generate TOTP QR code SVG
     *
     * @param issuer Issuer
     * @param username Username
     * @param secretKey Secret key
     * @return QR code as SVG string
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun generateQRCodeSVG(issuer: String, username: String, secretKey: String): String {
        val qrCode = generateQRCode(issuer, username, secretKey)
        val stringBuilder =
            StringBuilder("<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\" viewBox=\"0 0 ${qrCode.width} ${qrCode.height}\">")

        for (y in 0 until qrCode.height) {
            for (x in 0 until qrCode.width) {
                if (qrCode.get(x, y)) {
                    stringBuilder.appendLine("    <rect width=\"1\" height=\"1\" x=\"$x\" y=\"$y\"/>")
                }
            }
        }
        stringBuilder.append("</svg>")

        return stringBuilder.toString()
    }

    /**
     * Generate TOTP QR code SVG as Base64 string
     *
     * @param issuer Issuer
     * @param username Username
     * @param secretKey Secret key
     * @return TOTP QR code base64
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun generateQRCodeSVGBase64(issuer: String, username: String, secretKey: String) =
        Base64.encode(generateQRCodeSVG(issuer, username, secretKey).toByteArray(Charsets.UTF_8))
}
