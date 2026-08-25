package top.fatweb.apimanagement.converter.permission

import top.fatweb.apimanagement.entity.permission.Scope
import top.fatweb.apimanagement.vo.permission.base.ScopeVo

/**
 * Convert to ScopeVo object
 *
 * @return ScopeVo object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see Scope
 * @see ScopeVo
 */
fun Scope.toVo() = ScopeVo(
    id = this.id,
    name = this.name,
    menuId = this.menuId
)
