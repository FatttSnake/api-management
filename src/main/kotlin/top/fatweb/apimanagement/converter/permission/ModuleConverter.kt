package top.fatweb.apimanagement.converter.permission

import top.fatweb.apimanagement.entity.permission.Module
import top.fatweb.apimanagement.vo.permission.base.ModuleVo

/**
 * Convert to ModuleVo object
 *
 * @return ModuleVo object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see Module
 * @see ModuleVo
 */
fun Module.toVo() = ModuleVo(
    id = this.id,
    name = this.name
)
