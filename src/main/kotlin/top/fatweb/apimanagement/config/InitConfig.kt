package top.fatweb.apimanagement.config

import com.baomidou.mybatisplus.extension.kotlin.KtQueryWrapper
import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.core.token.Sha512DigestUtils
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import top.fatweb.apimanagement.entity.permission.User
import top.fatweb.apimanagement.entity.permission.UserInfo
import top.fatweb.apimanagement.properties.ServerProperties
import top.fatweb.apimanagement.service.permission.IUserInfoService
import top.fatweb.apimanagement.service.permission.IUserService
import top.fatweb.apimanagement.util.generateRandomPassword
import top.fatweb.avatargenerator.GitHubAvatar

/**
 * Application initialization configuration
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ServerProperties
 * @see PasswordEncoder
 * @see IUserService
 * @see IUserInfoService
 */
@Component
class InitConfig(
    private val serverProperties: ServerProperties,
    private val passwordEncoder: PasswordEncoder,
    private val userService: IUserService,
    private val userInfoService: IUserInfoService
) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    @PostConstruct
    fun init() {
        if (!userService.exists(KtQueryWrapper(User()).eq(User::id, 0))) {
            userInfoService.remove(KtQueryWrapper(UserInfo()).eq(UserInfo::userId, 0))

            val rawPassword = serverProperties.admin.password ?: let {
                logger.warn("No default administrator password is set, a randomly generated password will be used")
                generateRandomPassword(10)
            }
            val encodedPassword = passwordEncoder.encode(Sha512DigestUtils.shaHex(rawPassword))

            val user = User().apply {
                id = 0
                username = serverProperties.admin.username
                password = encodedPassword
                locking = 0
                enable = 1
            }
            val userInfo = UserInfo().apply {
                userId = 0
                nickname = serverProperties.admin.nickname
                avatar =
                    GitHubAvatar.newAvatarBuilder().build().createAsBase64((Long.MIN_VALUE..Long.MAX_VALUE).random())
                email = serverProperties.admin.email
            }

            if (userService.save(user) && userInfoService.save(userInfo)) {
                logger.warn("First startup, create administrator - username: admin, password: $rawPassword")
                logger.warn("This information will only be shown once. Please change your password promptly after logging in.")
            }
        }

    }
}
