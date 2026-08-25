package top.fatweb.apimanagement.converter.permission

import top.fatweb.apimanagement.entity.permission.*
import top.fatweb.apimanagement.vo.permission.PowerSetVo

/**
 * Convert to PowerSetVo object
 *
 * @return PowerSetVo object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see PowerSet
 * @see PowerSetVo
 */
fun PowerSet.toVo() = PowerSetVo(
    moduleList = this.moduleList?.map(Module::toVo),
    menuList = this.menuList?.map(Menu::toVo),
    scopeList = this.scopeLists?.map(Scope::toVo),
    operationList = this.operationList?.map(Operation::toVo)
)
