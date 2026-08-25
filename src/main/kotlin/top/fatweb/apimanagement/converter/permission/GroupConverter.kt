package top.fatweb.apimanagement.converter.permission

import com.baomidou.mybatisplus.core.metadata.IPage
import top.fatweb.apimanagement.entity.permission.Group
import top.fatweb.apimanagement.entity.permission.Role
import top.fatweb.apimanagement.param.permission.group.GroupAddParam
import top.fatweb.apimanagement.param.permission.group.GroupUpdateParam
import top.fatweb.apimanagement.param.permission.group.GroupUpdateStatusParam
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.permission.GroupWithRoleVo
import top.fatweb.apimanagement.vo.permission.base.GroupVo

/**
 * Convert to GroupVo object
 *
 * @return GroupVo object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see Group
 * @see GroupVo
 */
fun Group.toVo() = GroupVo(
    id = this.id,
    name = this.name,
    enable = this.enable?.let { it == 1 },
    createTime = this.createTime,
    updateTime = this.updateTime
)

/**
 * Convert to GroupWithRoleVo object
 *
 * @return GroupWithRoleVo object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see Group
 * @see GroupWithRoleVo
 */
fun Group.toVoWithRole() = GroupWithRoleVo(
    id = this.id,
    name = this.name,
    enable = this.enable?.let { it == 1 },
    createTime = this.createTime,
    updateTime = this.updateTime,
    roles = this.roles?.map(Role::toVo)
)

/**
 * Convert to PageVo<GroupWithRoleVo> object
 *
 * @return PageVo<GroupWithRoleVo> object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see IPage
 * @see Group
 * @see PageVo
 */
fun IPage<Group>.toVoWithRole() = PageVo(
    total = this.total,
    pages = this.pages,
    size = this.size,
    current = this.current,
    records = this.records.map(Group::toVoWithRole)
)

/**
 * Convert to Group object
 *
 * @return Group object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see GroupAddParam
 * @see Group
 */
fun GroupAddParam.toEntity() = Group().apply {
    name = this@toEntity.name
    enable = if (this@toEntity.enable) 1 else 0
    roles = this@toEntity.roleIds?.map { Role().apply { id = it } }
}

/**
 * Convert to Group object
 *
 * @return Group object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see GroupUpdateParam
 * @see Group
 */
fun GroupUpdateParam.toEntity() = Group().apply {
    id = this@toEntity.id
    name = this@toEntity.name
    enable = if (this@toEntity.enable) 1 else 0
    roles = this@toEntity.roleIds?.map { Role().apply { id = it } }
}

/**
 * Convert to Group object
 *
 * @return Group object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see GroupUpdateStatusParam
 * @see Group
 */
fun GroupUpdateStatusParam.toEntity() = Group().apply {
    id = this@toEntity.id
    enable = if (this@toEntity.enable) 1 else 0
}
