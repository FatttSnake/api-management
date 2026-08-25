package top.fatweb.apimanagement.converter.permission

import com.baomidou.mybatisplus.core.metadata.IPage
import top.fatweb.apimanagement.entity.permission.*
import top.fatweb.apimanagement.param.permission.role.RoleAddParam
import top.fatweb.apimanagement.param.permission.role.RoleUpdateParam
import top.fatweb.apimanagement.param.permission.role.RoleUpdateStatusParam
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.permission.RoleWithPowerVo
import top.fatweb.apimanagement.vo.permission.base.RoleVo

/**
 * Convert to RoleVo object
 *
 * @return RoleVo object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see Role
 * @see RoleVo
 */
fun Role.toVo() = RoleVo(
    id = this.id,
    name = this.name,
    enable = this.enable?.let { it == 1 },
    createTime = this.createTime,
    updateTime = this.updateTime
)

/**
 * Convert to RoleWithPowerVo object
 *
 * @return RoleWithPowerVo object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see Role
 * @see RoleWithPowerVo
 */
fun Role.toVoWithPower() = RoleWithPowerVo(
    id = this.id,
    name = this.name,
    enable = this.enable?.let { it == 1 },
    createTime = this.createTime,
    updateTime = this.updateTime,
    modules = this.modules?.map(Module::toVo),
    menus = this.menus?.map(Menu::toVo),
    scopes = this.scopes?.map(Scope::toVo),
    operations = this.operations?.map(Operation::toVo)
)

/**
 * Convert to PageVo object
 *
 * @return PageVo<RoleWithPowerVo> object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see IPage
 * @see Role
 * @see PageVo
 */
fun IPage<Role>.toVoWithPower() = PageVo(
    total = this.total,
    pages = this.pages,
    size = this.size,
    current = this.current,
    records = this.records.map(Role::toVoWithPower)
)

/**
 * Convert to Role object
 *
 * @return Role object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see RoleAddParam
 * @see Role
 */
fun RoleAddParam.toEntity() = Role().apply {
    name = this@toEntity.name
    enable = if (this@toEntity.enable) 1 else 0
    powers = this@toEntity.powerIds?.map { Power().apply { id = it } }
}

/**
 * Convert to Role object
 *
 * @return Role object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see RoleUpdateParam
 * @see Role
 */
fun RoleUpdateParam.toEntity() = Role().apply {
    id = this@toEntity.id
    name = this@toEntity.name
    enable = if (this@toEntity.enable) 1 else 0
    powers = this@toEntity.powerIds?.map { Power().apply { id = it } }
}

/**
 * Convert to Role object
 *
 * @return Role object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see RoleUpdateStatusParam
 * @see Role
 */
fun RoleUpdateStatusParam.toEntity() = Role().apply {
    id = this@toEntity.id
    enable = if (this@toEntity.enable) 1 else 0
}
