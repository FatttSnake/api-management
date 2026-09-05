package top.fatweb.apimanagement.mapper.permission

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import com.baomidou.mybatisplus.core.metadata.IPage
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import top.fatweb.apimanagement.entity.permission.User

/**
 * User mapper
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see BaseMapper
 * @see User
 */
@Mapper
interface UserMapper : BaseMapper<User> {
    /**
     * Select one user with power and information by username or email
     *
     * @param account Username or email
     * @return User object with power and information
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see User
     */
    fun selectOneWithPowerInfoByAccount(@Param("account") account: String): User?

    /**
     * Select one user with power and information by ID
     *
     * @param id User ID
     * @return User object with power and information
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see User
     */
    fun selectOneWithPowerInfoById(@Param("id") id: Long): User?

    /**
     * Select one user with basic information by username
     *
     * @param username Username
     * @return User object with basic information
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see User
     */
    fun selectOneWithBasicInfoByUsername(@Param("username") username: String): User?

    /**
     * Select one user with basic information by ID
     *
     * @param id User ID
     * @return User object with basic information
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see User
     */
    fun selectOneWithBasicInfoById(@Param("id") id: Long): User?

    /**
     * Select user ID in page
     *
     * @param page Pagination
     * @param searchType Type of search
     * @param searchValue Value to search for
     * @param searchRegex Use regex
     * @return User in page
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see IPage
     */
    fun selectPage(
        page: IPage<Long>,
        @Param("searchType") searchType: String,
        @Param("searchValue") searchValue: String?,
        @Param("searchRegex") searchRegex: Boolean
    ): IPage<Long>

    /**
     * Select user with role and information list by list of user IDs
     *
     * @param userIds List of user IDs
     * @return User with role and information list
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see User
     */
    fun selectListWithRoleInfoByIds(@Param("userIds") userIds: List<Long>): List<User>

    /**
     * Select one user by ID
     *
     * @param id User ID
     * @return User object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see User
     */
    fun selectOneWithRoleInfoById(@Param("id") id: Long): User?

    /**
     * Select all user with information list
     *
     * @return User with information list
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see User
     */
    fun selectListWithInfo(): List<User>

    /**
     * Select user IDs list by list of role IDs
     *
     * @param roleIds List of role IDs
     * @return User IDs list
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun selectIdsWithRoleIds(@Param("roleIds") roleIds: List<Long>): List<Long>

    /**
     * Select user IDs list by list of group IDs
     *
     * @param groupIds List of group IDs
     * @return User IDs list
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun selectIdsWithGroupIds(@Param("groupIds") groupIds: List<Long>): List<Long>
}
