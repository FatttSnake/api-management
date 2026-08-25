package top.fatweb.apimanagement.service.permission.impl

import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Service
import top.fatweb.apimanagement.entity.permission.LoginUser
import top.fatweb.apimanagement.exception.UserNotFoundException
import top.fatweb.apimanagement.service.permission.IUserService
import top.fatweb.apimanagement.util.queryOrThrowException

/**
 * User details service implement
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see IUserService
 * @see UserDetailsService
 */
@Service
class UserDetailsServiceImpl(
    private val userService: IUserService
) : UserDetailsService {
    override fun loadUserByUsername(account: String): UserDetails {
        val user = queryOrThrowException(UserNotFoundException()) { userService.getUserWithPowerByAccount(account) }

        return LoginUser(user)
    }
}
