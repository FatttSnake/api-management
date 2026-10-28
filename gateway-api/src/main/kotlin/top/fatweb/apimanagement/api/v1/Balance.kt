package top.fatweb.apimanagement.api.v1

import java.math.BigDecimal

/**
 * Account balance snapshot.
 *
 * @property userId user ID
 * @property available available amount
 * @property frozen frozen amount
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
data class Balance @JvmOverloads constructor(
    val userId: Long,
    val available: BigDecimal = BigDecimal.ZERO,
    val frozen: BigDecimal = BigDecimal.ZERO
)
