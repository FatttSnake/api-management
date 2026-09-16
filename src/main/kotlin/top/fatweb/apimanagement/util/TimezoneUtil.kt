package top.fatweb.apimanagement.util

import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Time zone utility
 *
 * Times are stored in UTC, so every calendar day computed by the server has to be shifted by the
 * offset of the client to match the day the console shows
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
object TimezoneUtil {
    /**
     * Header carrying the time zone offset of the client in minutes, keep in sync with the allowed
     * headers of the CORS configuration
     */
    const val TIMEZONE_OFFSET_HEADER = "X-Timezone-Offset"

    private const val MIN_OFFSET_MINUTES = -720
    private const val MAX_OFFSET_MINUTES = 840

    /**
     * Get time zone offset of the client currently calling api
     *
     * The returned value is always clamped into the valid range, never pass the raw header to a
     * caller so that it stays safe to interpolate into SQL
     *
     * @return Offset in minutes, 0 when called outside a request or when the header is missing or invalid
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun offsetMinutes(): Int =
        (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)
            ?.request
            ?.getHeader(TIMEZONE_OFFSET_HEADER)
            ?.toIntOrNull()
            ?.coerceIn(MIN_OFFSET_MINUTES, MAX_OFFSET_MINUTES)
            ?: 0

    /**
     * Get current time of the client currently calling api
     *
     * @return Local time of the client
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun nowLocal(): LocalDateTime = LocalDateTime.now(ZoneOffset.UTC).plusMinutes(offsetMinutes().toLong())

    /**
     * Get start of the local day of the client currently calling api, expressed in UTC so that it
     * can be compared with the stored columns
     *
     * @param offsetDays Days to shift the local day by, negative for a day in the past
     * @return Start of the local day in UTC
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun startOfLocalDayUtc(offsetDays: Long = 0): LocalDateTime {
        val offset = offsetMinutes().toLong()

        return LocalDateTime.now(ZoneOffset.UTC)
            .plusMinutes(offset)
            .toLocalDate()
            .minusDays(offsetDays)
            .atStartOfDay()
            .minusMinutes(offset)
    }
}
