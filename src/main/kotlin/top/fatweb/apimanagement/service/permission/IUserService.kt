package top.fatweb.apimanagement.service.permission

import com.baomidou.mybatisplus.spring.service.IService
import top.fatweb.apimanagement.entity.permission.User
import top.fatweb.apimanagement.param.permission.user.*
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.permission.UserWithInfoVo
import top.fatweb.apimanagement.vo.permission.UserWithPowerInfoVo
import top.fatweb.apimanagement.vo.permission.UserWithRoleInfoVo

/**
 * User service interface
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
interface IUserService : IService<User> {
    /**
     * Generate random avatar as base64
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun generateAvatar(avatarGenerateParam: AvatarGenerateParam?): String

    /**
     * Get user with power by username or email
     *
     * @param account Username or email
     * @return User object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun getUserWithPowerByAccount(account: String): User?

    /**
     * Get user information
     *
     * @return UserWithPowerInfoVo object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun getInfo(): UserWithPowerInfoVo

    /**
     * Get user information by username
     *
     * @param username Username
     * @return UserWithInfoVo object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun getBasicInfo(username: String): UserWithInfoVo

    /**
     * Get user information by ID
     *
     * @param userId User ID
     * @return UserWithInfoVo object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun getBasicInfoById(userId: Long): UserWithInfoVo

    /**
     * Get user information by IDs
     *
     * @param userIds User ID List
     * @return Map<Long, UserWithInfoVo> object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun getBasicInfoByIds(userIds: List<Long>): Map<Long, UserWithInfoVo>

    /**
     * Update user information
     *
     * @param userInfoUpdateParam Update user information parameters
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun updateInfo(userInfoUpdateParam: UserInfoUpdateParam)

    /**
     * Change password
     *
     * @param userChangePasswordParam Change password parameters
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun password(userChangePasswordParam: UserChangePasswordParam)

    /**
     * Get one user by ID
     *
     * @param id ID
     * @return UserWithRoleInfoVo object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun getOne(id: Long): UserWithRoleInfoVo

    /**
     * Get user in page
     *
     * @param userGetParam Get user parameters
     * @return PageVo<UserWithRoleInfoVo> object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun getPage(userGetParam: UserGetParam?): PageVo<UserWithRoleInfoVo>

    /**
     * Get all user as list
     *
     * @return List of UserWithInfoVo object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun getList(): List<UserWithInfoVo>

    /**
     * Add user
     *
     * @param userAddParam Add user parameters
     * @return UserWithRoleInfoVo object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun add(userAddParam: UserAddParam): UserWithRoleInfoVo

    /**
     * Update user
     *
     * @param userUpdateParam Update user parameters
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun update(userUpdateParam: UserUpdateParam)

    /**
     * Update user password
     *
     * @param userUpdatePasswordParam Update user password parameters
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun password(userUpdatePasswordParam: UserUpdatePasswordParam)

    /**
     * Delete user by ID
     *
     * @param id ID
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun deleteOne(id: Long)

    /**
     * Delete user by list
     *
     * @param userDeleteParam Delete user parameters
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun delete(userDeleteParam: UserDeleteParam)

    /**
     * Get user IDs list by list of role IDs
     *
     * @param roleIds List of role IDs
     * @return User IDs list
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun getIdsByRoleIds(roleIds: List<Long>): List<Long>

    /**
     * Get user IDs list by list of group IDs
     *
     * @param groupIds List of group IDs
     * @return User IDs list
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun getIdsByGroupIds(groupIds: List<Long>): List<Long>
}
