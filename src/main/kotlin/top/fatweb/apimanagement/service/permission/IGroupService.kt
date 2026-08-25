package top.fatweb.apimanagement.service.permission

import com.baomidou.mybatisplus.spring.service.IService
import top.fatweb.apimanagement.entity.permission.Group
import top.fatweb.apimanagement.param.permission.group.*
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.permission.GroupWithRoleVo
import top.fatweb.apimanagement.vo.permission.base.GroupVo

/**
 * Group service interface
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see IService
 * @see Group
 */
interface IGroupService : IService<Group> {
    /**
     * Get one group by ID
     *
     * @param id ID
     * @return GroupWithRoleVo object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see GroupWithRoleVo
     */
    fun getOne(id: Long): GroupWithRoleVo

    /**
     * Get group in page
     *
     * @param groupGetParam Get group parameters
     * @return PageVo<GroupWithRoleVo> object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see GroupGetParam
     * @see PageVo
     * @see GroupWithRoleVo
     */
    fun getPage(groupGetParam: GroupGetParam?): PageVo<GroupWithRoleVo>

    /**
     * Get all group in list
     *
     * @return List of GroupVo object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see GroupVo
     */
    fun getList(): List<GroupVo>

    /**
     * Add group
     *
     * @param groupAddParam Add group parameters
     * @return GroupVo object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see GroupAddParam
     * @see GroupVo
     */
    fun add(groupAddParam: GroupAddParam): GroupVo

    /**
     * Update group
     *
     * @param groupUpdateParam Update group parameters
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see GroupUpdateParam
     */
    fun update(groupUpdateParam: GroupUpdateParam)

    /**
     * Update status of group
     *
     * @param groupUpdateStatusParam Update status of group parameters
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see GroupUpdateStatusParam
     */
    fun status(groupUpdateStatusParam: GroupUpdateStatusParam)

    /**
     * Delete group by ID
     *
     * @param id ID
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun deleteOne(id: Long)

    /**
     * Delete group by list
     *
     * @param groupDeleteParam Delete group parameters
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see GroupDeleteParam
     */
    fun delete(groupDeleteParam: GroupDeleteParam)
}
