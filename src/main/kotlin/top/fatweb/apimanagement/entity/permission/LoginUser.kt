package top.fatweb.apimanagement.entity.permission

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Login user entity
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see UserDetails
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
class LoginUser() : UserDetails {
    /**
     * User object
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see User
     */
    lateinit var user: User

    @JsonIgnore
    private var authorities: List<GrantedAuthority>? = null

    constructor(user: User) : this() {
        this.user = user
    }

    @JsonIgnore
    override fun getAuthorities(): List<GrantedAuthority> {
        authorities?.let { return it }
        authorities = user.operations?.map { SimpleGrantedAuthority(it.code!!) } ?: emptyList()

        return authorities as List<GrantedAuthority>
    }

    @JsonIgnore
    override fun getPassword() = user.password

    @JsonIgnore
    override fun getUsername() = user.username!!

    @JsonIgnore
    override fun isAccountNonExpired() =
        user.expiration == null || user.expiration!!.isAfter(LocalDateTime.now(ZoneOffset.UTC))

    @JsonIgnore
    override fun isAccountNonLocked() = user.locking == 0

    @JsonIgnore
    override fun isCredentialsNonExpired() =
        user.credentialsExpiration == null || user.credentialsExpiration!!.isAfter(LocalDateTime.now(ZoneOffset.UTC))

    @JsonIgnore
    override fun isEnabled() = user.enable == 1

    override fun toString(): String {
        return "LoginUser(user=$user, authorities=$authorities)"
    }
}
