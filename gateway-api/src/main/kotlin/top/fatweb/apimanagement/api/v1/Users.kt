package top.fatweb.apimanagement.api.v1

import java.math.BigDecimal

/**
 * Read-only platform user data.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
interface Users {
    /**
     * Finds a user.
     *
     * @param userId user ID
     * @return user snapshot, or null when absent
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun findUser(userId: Long): UserInfo?

    /**
     * Returns a user's available balance.
     *
     * @param userId user ID
     * @return available balance
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun getBalance(userId: Long): BigDecimal
}
