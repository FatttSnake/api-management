package top.fatweb.apimanagement.converter.permission

import com.baomidou.mybatisplus.core.metadata.IPage
import top.fatweb.apimanagement.entity.permission.*
import top.fatweb.apimanagement.param.permission.user.UserAddParam
import top.fatweb.apimanagement.param.permission.user.UserUpdateParam
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.permission.UserWithInfoVo
import top.fatweb.apimanagement.vo.permission.UserWithPowerInfoVo
import top.fatweb.apimanagement.vo.permission.UserWithRoleInfoVo
import top.fatweb.avatargenerator.GitHubAvatar

/**
 * Convert to UserWithPowerInfoVo object
 *
 * @return UserWithPowerInfoVo object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see User
 * @see UserWithPowerInfoVo
 */
fun User.toVoWithPowerInfo() = UserWithPowerInfoVo(
    id = this.id,
    username = this.username,
    twoFactor = !this.twoFactor.isNullOrBlank() && !this.twoFactor!!.endsWith("?"),
    verified = this.verify.isNullOrBlank(),
    locking = this.locking?.let { it == 1 },
    expiration = this.expiration,
    credentialsExpiration = this.credentialsExpiration,
    enable = this.enable?.let { it == 1 },
    currentLoginTime = this.currentLoginTime,
    currentLoginIp = this.currentLoginIp,
    lastLoginTime = this.lastLoginTime,
    lastLoginIp = this.lastLoginIp,
    createTime = this.createTime,
    updateTime = this.updateTime,
    userInfo = this.userInfo?.let(UserInfo::toVo),
    modules = this.modules?.map(Module::toVo),
    menus = this.menus?.map(Menu::toVo),
    scopes = this.scopes?.map(Scope::toVo),
    operations = this.operations?.map(Operation::toVo)
)

/**
 * Convert to UserWithRoleInfoVo object
 *
 * @return UserWithRoleInfoVo object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see User
 * @see UserWithRoleInfoVo
 */
fun User.toVoWithRoleInfo() = UserWithRoleInfoVo(
    id = this.id,
    username = this.username,
    twoFactor = !this.twoFactor.isNullOrBlank() && !this.twoFactor!!.endsWith("?"),
    verify = this.verify,
    locking = this.locking?.let { it == 1 },
    expiration = this.expiration,
    credentialsExpiration = this.credentialsExpiration,
    enable = this.enable?.let { it == 1 },
    currentLoginTime = this.currentLoginTime,
    currentLoginIp = this.currentLoginIp,
    lastLoginTime = this.lastLoginTime,
    lastLoginIp = this.lastLoginIp,
    createTime = this.createTime,
    updateTime = this.updateTime,
    userInfo = this.userInfo?.let(UserInfo::toVo),
    roles = this.roles?.map(Role::toVo),
    groups = this.groups?.map(Group::toVo)
)

/**
 * Convert to UserWithInfoVo object
 *
 * @return UserWithInfoVo object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see User
 * @see UserWithInfoVo
 */
fun User.toVoWithInfo() = UserWithInfoVo(
    id = this.id,
    username = this.username,
    twoFactor = !this.twoFactor.isNullOrBlank() && !this.twoFactor!!.endsWith("?"),
    verified = this.verify.isNullOrBlank(),
    locking = this.locking?.let { it == 1 },
    expiration = this.expiration,
    credentialsExpiration = this.credentialsExpiration,
    enable = this.enable?.let { it == 1 },
    currentLoginTime = this.currentLoginTime,
    currentLoginIp = this.currentLoginIp,
    lastLoginTime = this.lastLoginTime,
    lastLoginIp = this.lastLoginIp,
    createTime = this.createTime,
    updateTime = this.updateTime,
    userInfo = this.userInfo?.let(UserInfo::toVo)
)

/**
 * Convert to User object
 *
 * @return User object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see UserAddParam
 * @see User
 */
fun UserAddParam.toEntity() = User().apply {
    username = this@toEntity.username
    password = this@toEntity.password
    locking = if (this@toEntity.locking) 1 else 0
    expiration = this@toEntity.expiration
    credentialsExpiration = this@toEntity.credentialsExpiration
    enable = if (this@toEntity.enable) 1 else 0
    userInfo = UserInfo().apply {
        nickname = this@toEntity.nickname ?: this@toEntity.username
        avatar = this@toEntity.avatar ?: GitHubAvatar.newAvatarBuilder().build()
            .createAsBase64((Long.MIN_VALUE..Long.MAX_VALUE).random())
        email = this@toEntity.email
    }
    roles = this@toEntity.roleIds?.map { Role().apply { id = it } }
    groups = this@toEntity.groupIds?.map { Group().apply { id = it } }
}

/**
 * Convert to User object
 *
 * @return User object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see UserUpdateParam
 * @see User
 */
fun UserUpdateParam.toEntity() = User().apply {
    id = this@toEntity.id
    username = this@toEntity.username
    locking = if (this@toEntity.locking && this@toEntity.id != 0L) 1 else 0
    expiration = if (this@toEntity.id != 0L) this@toEntity.expiration else null
    credentialsExpiration = this@toEntity.credentialsExpiration
    enable = if (this@toEntity.enable || this@toEntity.id == 0L) 1 else 0
    userInfo = UserInfo().apply {
        nickname = this@toEntity.nickname
        avatar = this@toEntity.avatar
        email = this@toEntity.email
    }
    roles = if (this@toEntity.id != 0L) this@toEntity.roleIds?.map { Role().apply { id = it } } else null
    groups = if (this@toEntity.id != 0L) this@toEntity.groupIds?.map { Group().apply { id = it } } else null
}

/**
 * Convert to PageVo<UserWithRoleInfoVo> object
 *
 * @return PageVo<UserWithRoleInfoVo> object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see IPage
 * @see User
 * @see PageVo
 */
fun IPage<User>.toVoWithRoleInfo() = PageVo(
    total = this.total,
    pages = this.pages,
    size = this.size,
    current = this.current,
    records = this.records.map(User::toVoWithRoleInfo)
)
