package top.fatweb.apimanagement.mapper.permission

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import com.baomidou.mybatisplus.core.metadata.IPage
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import top.fatweb.apimanagement.entity.permission.Role

/**
 * Role mapper
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see BaseMapper
 * @see Role
 */
@Mapper
interface RoleMapper : BaseMapper<Role> {
    /**
     * Select role in page
     *
     * @param page Pagination
     * @param searchName Name to search for
     * @param searchRegex Use regex
     * @return Role in page
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see IPage
     */
    fun selectPage(
        page: IPage<Long>,
        @Param("searchName") searchName: String?,
        @Param("searchRegex") searchRegex: Boolean
    ): IPage<Long>

    /**
     * Select role with power list by list of role IDs
     *
     * @param roleIds List of role IDs
     * @return Role with power list
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see Role
     */
    fun selectListWithPowerByIds(@Param("roleIds") roleIds: List<Long>): List<Role>

    /**
     * Select one role by ID
     *
     * @param id Role ID
     * @return Role object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see Role
     */
    fun selectOneById(@Param("id") id: Long): Role?
}
