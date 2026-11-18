package top.fatweb.apimanagement.component.ratelimit

import org.springframework.stereotype.Component
import top.fatweb.apimanagement.entity.api.ApiInterface
import top.fatweb.apimanagement.entity.api.ApiKeyPrincipal
import top.fatweb.apimanagement.entity.api.ApiPlugin
import top.fatweb.apimanagement.settings.ApiSettings
import top.fatweb.apimanagement.settings.SettingsOperator

/**
 * Effective rate limit and quota policy backed by interface, key and system settings
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.2
 */
@Component
class RateLimitPolicy {
    /**
     * Per-API total limit shared by all callers
     *
     * @param api interface row
     * @param plugin owning plugin row
     * @return effective limit, 0 when unlimited
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.2
     */
    fun apiTotal(api: ApiInterface, plugin: ApiPlugin): Int =
        RateLimitResolver.resolveChildWithParent(api.rateLimit, plugin.defaultRateLimit)

    /**
     * Per-user aggregate limit across all keys and login-state calls
     *
     * @return effective limit, 0 when unlimited
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.2
     */
    fun userTotal(): Int =
        RateLimitResolver.resolveTopLevel(SettingsOperator.getValue(ApiSettings::defaultUserRateLimitPerMin, 0))

    /**
     * Per-IP aggregate limit for anonymous callers
     *
     * @return effective limit, 0 when unlimited
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.2
     */
    fun anonymousIpTotal(): Int =
        RateLimitResolver.resolveTopLevel(SettingsOperator.getValue(ApiSettings::defaultAnonymousRateLimitPerMin, 0))

    /**
     * Per-key limit, capped by the per-user aggregate
     *
     * @param principal API key principal
     * @return effective limit, 0 when unlimited
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.2
     */
    fun keyTotal(principal: ApiKeyPrincipal): Int =
        RateLimitResolver.resolveChildWithParent(principal.rateLimit, userTotal())

    /**
     * Per-IP authentication failure limit
     *
     * @return effective limit, 0 when disabled
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.2
     */
    fun authFailureTotal(): Int = RateLimitResolver.resolveAuthFailureLimit(
        SettingsOperator.getValue(
            ApiSettings::authFailureLimitPerMin,
            RateLimitResolver.DEFAULT_AUTH_FAILURE_LIMIT,
        ),
    )

    /**
     * Per-user quota across all keys and login-state calls
     *
     * @return effective quota, 0 when unlimited
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.2
     */
    fun userQuota(): Long = RateLimitResolver.resolveTopLevel(SettingsOperator.getValue(ApiSettings::defaultQuota, 0L))

    /**
     * Period of the per-user quota
     *
     * @return period in seconds
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.2
     */
    fun userQuotaPeriod(): Int = SettingsOperator.getValue(ApiSettings::defaultQuotaPeriodSeconds, 86400)

    /**
     * Optional per-key quota
     *
     * @param principal API key principal
     * @return effective quota, 0 when no per-key cap
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.2
     */
    fun keyQuota(principal: ApiKeyPrincipal): Long = principal.quota?.takeIf { it > 0 } ?: 0L

    /**
     * Period of the optional per-key quota
     *
     * @param principal API key principal
     * @return period in seconds
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.2
     */
    fun keyQuotaPeriod(principal: ApiKeyPrincipal): Int =
        RateLimitResolver.resolveQuotaPeriod(principal.quotaPeriod, userQuotaPeriod())
}
