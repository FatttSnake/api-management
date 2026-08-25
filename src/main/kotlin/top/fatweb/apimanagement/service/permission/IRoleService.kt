package top.fatweb.apimanagement.service.permission

import com.baomidou.mybatisplus.spring.service.IService
import top.fatweb.apimanagement.entity.permission.Role
import top.fatweb.apimanagement.param.permission.role.*
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.permission.RoleWithPowerVo
import top.fatweb.apimanagement.vo.permission.base.RoleVo

/**
 * Role service interface
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see IService
 * @see Role
 */
interface IRoleService : IService<Role> {
    /**
     * Get user by ID
     *
     * @param id ID
     * @return RoleWithPowerVo object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see RoleWithPowerVo
     */
    fun getOne(id: Long): RoleWithPowerVo

    /**
     * Get role in page
     *
     * @param roleGetParam Get role parameters
     * @return PageVo<RoleWithPowerVo> object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see RoleGetParam
     * @see PageVo
     * @see RoleWithPowerVo
     */
    fun getPage(roleGetParam: RoleGetParam?): PageVo<RoleWithPowerVo>

    /**
     * Get all role in list
     *
     * @return List of RoleVo object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see RoleVo
     */
    fun getList(): List<RoleVo>

    /**
     * Add role
     *
     * @param roleAddParam Add role parameters
     * @return RoleVo object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see RoleAddParam
     * @see RoleVo
     */
    fun add(roleAddParam: RoleAddParam): RoleVo

    /**
     * Update role
     *
     * @param roleUpdateParam Update role parameters
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see RoleUpdateParam
     */
    fun update(roleUpdateParam: RoleUpdateParam)

    /**
     * Update status of role
     *
     * @param roleUpdateStatusParam Update status of role parameters
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see RoleUpdateStatusParam
     */
    fun status(roleUpdateStatusParam: RoleUpdateStatusParam)

    /**
     * Delete role by ID
     *
     * @param id ID
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun deleteOne(id: Long)

    /**
     * Delete role by list
     *
     * @param roleDeleteParam Delete role parameters
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see RoleDeleteParam
     */
    fun delete(roleDeleteParam: RoleDeleteParam)
}
