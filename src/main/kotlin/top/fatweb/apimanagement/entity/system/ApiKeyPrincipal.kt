package top.fatweb.apimanagement.entity.system

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

/**
 * API key principal
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see UserDetails
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
class ApiKeyPrincipal : UserDetails {
    /**
     * API key ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    var keyId: Long? = null

    /**
     * Owner user ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    var userId: Long? = null

    /**
     * Access key
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    var accessKey: String? = null

    /**
     * Enabled status
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    var status: Int? = null

    /**
     * Scoped API codes
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    var permissions: List<String> = emptyList()

    /**
     * Per-key rate limit per minute
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    var rateLimit: Int? = null

    /**
     * Quota requests per period
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    var quota: Long? = null

    /**
     * Quota period in seconds
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    var quotaPeriod: Int? = null

    @JsonIgnore
    override fun getAuthorities(): List<GrantedAuthority> = permissions.map { SimpleGrantedAuthority(it) }

    @JsonIgnore
    override fun getPassword(): String? = null

    @JsonIgnore
    override fun getUsername(): String = accessKey ?: ""

    @JsonIgnore
    override fun isAccountNonExpired(): Boolean = true

    @JsonIgnore
    override fun isAccountNonLocked(): Boolean = status == 1

    @JsonIgnore
    override fun isCredentialsNonExpired(): Boolean = true

    @JsonIgnore
    override fun isEnabled(): Boolean = status == 1
}
