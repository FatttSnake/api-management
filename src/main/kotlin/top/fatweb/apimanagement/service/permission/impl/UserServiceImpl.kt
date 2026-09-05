package top.fatweb.apimanagement.service.permission.impl

import com.baomidou.dynamic.datasource.annotation.DS
import com.baomidou.mybatisplus.core.metadata.OrderItem
import com.baomidou.mybatisplus.extension.kotlin.KtQueryWrapper
import com.baomidou.mybatisplus.extension.kotlin.KtUpdateWrapper
import com.baomidou.mybatisplus.extension.plugins.pagination.Page
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.token.Sha512DigestUtils
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import top.fatweb.apimanagement.component.storage.RedisProvider
import top.fatweb.apimanagement.converter.permission.toEntity
import top.fatweb.apimanagement.converter.permission.toVoWithInfo
import top.fatweb.apimanagement.converter.permission.toVoWithPowerInfo
import top.fatweb.apimanagement.converter.permission.toVoWithRoleInfo
import top.fatweb.apimanagement.entity.permission.RUserGroup
import top.fatweb.apimanagement.entity.permission.RUserRole
import top.fatweb.apimanagement.entity.permission.User
import top.fatweb.apimanagement.entity.permission.UserInfo
import top.fatweb.apimanagement.exception.UserNotFoundException
import top.fatweb.apimanagement.mapper.permission.UserMapper
import top.fatweb.apimanagement.param.permission.user.*
import top.fatweb.apimanagement.properties.ServerProperties
import top.fatweb.apimanagement.service.permission.*
import top.fatweb.apimanagement.util.*
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.permission.UserWithInfoVo
import top.fatweb.apimanagement.vo.permission.UserWithPowerInfoVo
import top.fatweb.apimanagement.vo.permission.UserWithRoleInfoVo
import top.fatweb.avatargenerator.GitHubAvatar
import top.fatweb.avatargenerator.IdenticonAvatar
import top.fatweb.avatargenerator.SquareAvatar
import top.fatweb.avatargenerator.TriangleAvatar
import top.fatweb.avatargenerator.layer.background.ColorPaintBackgroundLayer
import java.awt.Color
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import kotlin.io.encoding.Base64

/**
 * User service implement
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ServerProperties
 * @see PasswordEncoder
 * @see RedisProvider
 * @see IUserInfoService
 * @see IModuleService
 * @see IMenuService
 * @see IScopeService
 * @see IOperationService
 * @see IRUserRoleService
 * @see IRUserGroupService
 * @see ServiceImpl
 * @see UserMapper
 * @see User
 * @see IUserService
 */
@DS("master")
@Service
class UserServiceImpl(
    private val serverProperties: ServerProperties,
    private val passwordEncoder: PasswordEncoder,
    private val redisProvider: RedisProvider,
    private val userInfoService: IUserInfoService,
    private val moduleService: IModuleService,
    private val menuService: IMenuService,
    private val scopeService: IScopeService,
    private val operationService: IOperationService,
    private val rUserRoleService: IRUserRoleService,
    private val rUserGroupService: IRUserGroupService
) : ServiceImpl<UserMapper, User>(), IUserService {
    override fun generateAvatar(avatarGenerateParam: AvatarGenerateParam?): String = when ((1..4).random()) {
        1 -> AvatarGenerator.triangleBase64(avatarGenerateParam)
        2 -> AvatarGenerator.squareBase64(avatarGenerateParam)
        3 -> AvatarGenerator.identiconBase64(avatarGenerateParam)
        else -> AvatarGenerator.githubBase64(avatarGenerateParam)
    }

    override fun getUserWithPowerByAccount(account: String): User? {
        val user = baseMapper.selectOneWithPowerInfoByAccount(account) ?: return null

        if (user.id == 0L) {
            user.modules = moduleService.list()
            user.menus = menuService.list()
            user.scopes = scopeService.list()
            user.operations = operationService.list()
        }

        return user
    }

    override fun getInfo(): UserWithPowerInfoVo =
        queryOrThrowException(UserNotFoundException()) {
            getLoginUsername()?.let(::getUserWithPowerByAccount)
        }.let(User::toVoWithPowerInfo)

    override fun getBasicInfo(username: String): UserWithInfoVo =
        queryOrThrowException(UserNotFoundException()) {
            baseMapper.selectOneWithBasicInfoByUsername(username)
        }.let(User::toVoWithInfo)

    override fun getBasicInfoById(userId: Long): UserWithInfoVo =
        queryOrThrowException(UserNotFoundException()) {
            baseMapper.selectOneWithBasicInfoById(userId)
        }.let(User::toVoWithInfo)

    override fun updateInfo(userInfoUpdateParam: UserInfoUpdateParam) {
        val userId = getLoginUserIdOrThrow()
        updateOrThrowException {
            userInfoService.update(
                KtUpdateWrapper(UserInfo()).eq(UserInfo::userId, userId)
                    .set(!userInfoUpdateParam.avatar.isNullOrBlank(), UserInfo::avatar, userInfoUpdateParam.avatar)
                    .set(
                        !userInfoUpdateParam.nickname.isNullOrBlank(),
                        UserInfo::nickname,
                        userInfoUpdateParam.nickname
                    )
            )
        }
    }

    override fun password(userChangePasswordParam: UserChangePasswordParam) {
        val user = queryOrThrowException(UserNotFoundException()) {
            getById(
                getLoginUserIdOrThrow()
            )
        }

        if (!passwordEncoder.matches(userChangePasswordParam.originalPassword, user.password)) {
            throw BadCredentialsException("Passwords do not match")
        }

        updateOrThrowException {
            update(
                KtUpdateWrapper(User())
                    .eq(User::id, user.id)
                    .set(User::password, passwordEncoder.encode(userChangePasswordParam.newPassword))
                    .set(User::credentialsExpiration, null)
                    .set(User::updateTime, LocalDateTime.now(ZoneOffset.UTC))
            )
        }

        offlineUser(serverProperties = serverProperties, redisProvider = redisProvider, user.id!!)
    }

    override fun getOne(id: Long): UserWithRoleInfoVo =
        queryOrThrowException(UserNotFoundException()) {
            baseMapper.selectOneWithRoleInfoById(id)
        }.let(User::toVoWithRoleInfo)

    override fun getPage(userGetParam: UserGetParam?): PageVo<UserWithRoleInfoVo> {
        val userIdsPage = Page<Long>(userGetParam?.currentPage ?: 1, userGetParam?.pageSize ?: 20)

        setPageSort(userGetParam, userIdsPage, OrderItem.asc("id"))

        val userIdsIPage =
            baseMapper.selectPage(
                userIdsPage,
                userGetParam?.searchType ?: "ALL",
                userGetParam?.searchValue,
                userGetParam?.searchRegex ?: false
            )
        val userPage = Page<User>(userIdsIPage.current, userIdsIPage.size, userIdsIPage.total)
        if (userIdsIPage.total > 0) {
            userPage.records = baseMapper.selectListWithRoleInfoByIds(userIdsIPage.records)
        }

        return userPage.toVoWithRoleInfo()
    }

    override fun getList() = baseMapper.selectListWithInfo().map(User::toVoWithInfo)

    @Transactional
    override fun add(userAddParam: UserAddParam): UserWithRoleInfoVo {
        val newPassword =
            if (userAddParam.password.isNullOrBlank()) Sha512DigestUtils.shaHex(generateRandomPassword(10)) else userAddParam.password
        val user = userAddParam.toEntity()

        user.apply {
            password = passwordEncoder.encode(newPassword)
            verify = if (userAddParam.verified) null else "${
                LocalDateTime.now(ZoneOffset.UTC).toInstant(ZoneOffset.UTC).toEpochMilli()
            }-${UUID.randomUUID()}-${UUID.randomUUID()}-${UUID.randomUUID()}"
        }

        saveOrThrowException { save(user) }

        saveOrThrowException { user.userInfo!!.apply { userId = user.id }.let(userInfoService::save) }

        if (!user.roles.isNullOrEmpty()) {
            saveOrThrowException {
                rUserRoleService.saveBatch(user.roles!!.map {
                    RUserRole().apply {
                        userId = user.id
                        roleId = it.id
                    }
                })
            }
        }

        if (!user.groups.isNullOrEmpty()) {
            saveOrThrowException {
                rUserGroupService.saveBatch(user.groups!!.map {
                    RUserGroup().apply {
                        userId = user.id
                        groupId = it.id
                    }
                })
            }
        }

        return user.toVoWithRoleInfo()
    }

    @Transactional
    override fun update(userUpdateParam: UserUpdateParam) {
        val user = userUpdateParam.toEntity()
        user.updateTime = LocalDateTime.now(ZoneOffset.UTC)

        val oldRoleList = rUserRoleService.list(
            KtQueryWrapper(RUserRole())
                .select(RUserRole::roleId)
                .eq(RUserRole::userId, userUpdateParam.id)
        ).map(RUserRole::roleId)
        val addRoleIds = HashSet<Long>()
        val removeRoleIds = HashSet<Long>()
        userUpdateParam.roleIds?.forEach(addRoleIds::add)
        oldRoleList.forEach {
            it?.let(removeRoleIds::add)
        }
        removeRoleIds.removeAll(addRoleIds)
        oldRoleList.toSet().let(addRoleIds::removeAll)

        val oldGroupList = rUserGroupService.list(
            KtQueryWrapper(RUserGroup())
                .select(RUserGroup::groupId)
                .eq(RUserGroup::userId, userUpdateParam.id)
        ).map(RUserGroup::groupId)
        val addGroupIds = HashSet<Long>()
        val removeGroupIds = HashSet<Long>()
        userUpdateParam.groupIds?.forEach(addGroupIds::add)
        oldGroupList.forEach {
            it?.let(removeGroupIds::add)
        }
        removeGroupIds.removeAll(addGroupIds)
        oldGroupList.toSet().let(addGroupIds::removeAll)

        updateOrThrowException { updateById(user) }
        updateOrThrowException {
            update(
                KtUpdateWrapper(User()).eq(User::id, user.id)
                    .set(
                        User::verify,
                        if (userUpdateParam.verified || userUpdateParam.id == 0L) null else "${
                            LocalDateTime.now(ZoneOffset.UTC).toInstant(ZoneOffset.UTC).toEpochMilli()
                        }-${UUID.randomUUID()}-${UUID.randomUUID()}-${UUID.randomUUID()}"
                    )
                    .set(User::expiration, if (userUpdateParam.id == 0L) null else user.expiration)
                    .set(
                        User::credentialsExpiration,
                        if (userUpdateParam.id == 0L) null else user.credentialsExpiration
                    )
            )
        }

        user.userInfo?.let { userInfo ->
            userInfoService.getOne(
                KtQueryWrapper(UserInfo())
                    .select(UserInfo::id)
                    .eq(UserInfo::userId, userUpdateParam.id)
            )?.let {
                userInfo.id = it.id
                updateOrThrowException {
                    userInfoService.updateById(userInfo)
                }
            }
        }

        removeRoleIds.forEach {
            rUserRoleService.remove(
                KtQueryWrapper(RUserRole()).eq(
                    RUserRole::userId, userUpdateParam.id
                ).eq(RUserRole::roleId, it)
            )
        }

        addRoleIds.forEach {
            saveOrThrowException {
                rUserRoleService.save(RUserRole().apply {
                    userId = userUpdateParam.id
                    roleId = it
                })
            }
        }

        removeGroupIds.forEach {
            rUserGroupService.remove(
                KtQueryWrapper(RUserGroup()).eq(
                    RUserGroup::userId, userUpdateParam.id
                ).eq(RUserGroup::groupId, it)
            )
        }

        addGroupIds.forEach {
            saveOrThrowException {
                rUserGroupService.save(RUserGroup().apply {
                    userId = userUpdateParam.id
                    groupId = it
                })
            }
        }

        userUpdateParam.id?.let { offlineUser(serverProperties = serverProperties, redisProvider = redisProvider, it) }
    }

    override fun password(userUpdatePasswordParam: UserUpdatePasswordParam) {
        if (getLoginUserId() != 0L && userUpdatePasswordParam.id == 0L) {
            throw AccessDeniedException("Access denied")
        }

        val user = queryOrThrowException { getById(userUpdatePasswordParam.id) }
        updateOrThrowException {
            update(
                KtUpdateWrapper(User())
                    .eq(User::id, user.id)
                    .set(User::password, passwordEncoder.encode(userUpdatePasswordParam.password))
                    .set(
                        User::credentialsExpiration,
                        if (user.id != 0L) userUpdatePasswordParam.credentialsExpiration else null
                    )
                    .set(User::updateTime, LocalDateTime.now(ZoneOffset.UTC))
            )
        }

        userUpdatePasswordParam.id?.let {
            offlineUser(
                serverProperties = serverProperties,
                redisProvider = redisProvider,
                it
            )
        }
    }

    @Transactional
    override fun deleteOne(id: Long) {
        if (id == 0L) {
            return
        }

        delete(UserDeleteParam(listOf(id)))
    }

    @Transactional
    override fun delete(userDeleteParam: UserDeleteParam) {
        val ids = userDeleteParam.ids!!.filter { it != 0L }
        if (ids.isEmpty()) {
            return
        }

        removeBatchByIds(ids)
        userInfoService.remove(KtQueryWrapper(UserInfo()).`in`(UserInfo::userId, ids))
        rUserRoleService.remove(KtQueryWrapper(RUserRole()).`in`(RUserRole::userId, ids))
        rUserGroupService.remove(KtQueryWrapper(RUserGroup()).`in`(RUserGroup::userId, ids))

        offlineUser(serverProperties = serverProperties, redisProvider = redisProvider, *ids.toLongArray())
    }

    override fun getIdsByRoleIds(roleIds: List<Long>) = baseMapper.selectIdsWithRoleIds(roleIds)

    override fun getIdsByGroupIds(groupIds: List<Long>) = baseMapper.selectIdsWithGroupIds(groupIds)

    object AvatarGenerator {
        fun triangle(avatarGenerateParam: AvatarGenerateParam?): ByteArray {
            val avatar = (
                    if (avatarGenerateParam == null || avatarGenerateParam.colors.isNullOrEmpty())
                        TriangleAvatar.newAvatarBuilder()
                    else TriangleAvatar.newAvatarBuilder(
                        *avatarGenerateParam.colors!!.map(::decodeColor).toTypedArray()
                    )
                    ).apply {
                    avatarGenerateParam?.size?.let(::size)
                    avatarGenerateParam?.margin?.let(::margin)
                    avatarGenerateParam?.padding?.let(::padding)
                    avatarGenerateParam?.background?.let { layers(ColorPaintBackgroundLayer(decodeColor(it))) }
                }.build()

            return avatar.createAsPngBytes(avatarGenerateParam?.seed ?: getRandomLong())
        }

        fun triangleBase64(avatarGenerateParam: AvatarGenerateParam?) =
            Base64.encode(triangle(avatarGenerateParam))

        fun square(avatarGenerateParam: AvatarGenerateParam?): ByteArray {
            val avatar = (
                    if (avatarGenerateParam == null || avatarGenerateParam.colors.isNullOrEmpty())
                        SquareAvatar.newAvatarBuilder()
                    else SquareAvatar.newAvatarBuilder(
                        *avatarGenerateParam.colors!!.map(::decodeColor).toTypedArray()
                    )
                    ).apply {
                    avatarGenerateParam?.size?.let(::size)
                    avatarGenerateParam?.margin?.let(::margin)
                    avatarGenerateParam?.padding?.let(::padding)
                    avatarGenerateParam?.background?.let { layers(ColorPaintBackgroundLayer(decodeColor(it))) }
                }.build()

            return avatar.createAsPngBytes(avatarGenerateParam?.seed ?: getRandomLong())
        }

        fun squareBase64(avatarGenerateParam: AvatarGenerateParam?) =
            Base64.encode(square(avatarGenerateParam))

        fun identicon(avatarGenerateParam: AvatarGenerateParam?): ByteArray {
            val avatar = IdenticonAvatar.newAvatarBuilder().apply {
                avatarGenerateParam?.size?.let(::size)
                avatarGenerateParam?.margin?.let(::margin)
                avatarGenerateParam?.padding?.let(::padding)
                if (avatarGenerateParam != null && !avatarGenerateParam.colors.isNullOrEmpty()) {
                    color(decodeColor(avatarGenerateParam.colors!!.random()))
                }
                avatarGenerateParam?.background?.let { layers(ColorPaintBackgroundLayer(decodeColor(it))) }
            }.build()

            return avatar.createAsPngBytes(avatarGenerateParam?.seed ?: getRandomLong())
        }

        fun identiconBase64(avatarGenerateParam: AvatarGenerateParam?) =
            Base64.encode(identicon(avatarGenerateParam))

        fun github(avatarGenerateParam: AvatarGenerateParam?, elementSize: Int = 400, precision: Int = 5): ByteArray {
            val avatar = (avatarGenerateParam?.let { GitHubAvatar.newAvatarBuilder(elementSize, precision) }
                ?: let { GitHubAvatar.newAvatarBuilder(400, 5) }).apply {
                avatarGenerateParam?.size?.let(::size)
                avatarGenerateParam?.margin?.let(::margin)
                avatarGenerateParam?.padding?.let(::padding)
                if (avatarGenerateParam != null && !avatarGenerateParam.colors.isNullOrEmpty()) {
                    color(decodeColor(avatarGenerateParam.colors!!.random()))
                }
                avatarGenerateParam?.background?.let { layers(ColorPaintBackgroundLayer(decodeColor(it))) }
            }.build()

            return avatar.createAsPngBytes(avatarGenerateParam?.seed ?: getRandomLong())
        }

        fun githubBase64(avatarGenerateParam: AvatarGenerateParam?) =
            Base64.encode(github(avatarGenerateParam))

        private fun decodeColor(nm: String): Color {
            return if (Regex("^#[0-9a-fA-F]{6}$").matches(nm)) {
                Color.decode(nm)
            } else if (Regex("^#[0-9a-fA-F]{8}$").matches(nm)) {
                val intVal = Integer.decode(nm.substring(1..6).prependIndent("#"))
                val alpha = Integer.decode(nm.substring(7).prependIndent("#"))
                Color(intVal shr 16 and 0xFF, intVal shr 8 and 0XFF, intVal and 0xFF, alpha and 0xFF)
            } else {
                Color.WHITE
            }
        }
    }
}
