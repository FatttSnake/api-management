package top.fatweb.apimanagement.converter.permission

import top.fatweb.apimanagement.entity.permission.Operation
import top.fatweb.apimanagement.vo.permission.base.OperationVo

/**
 * Convert to OperationVo object
 *
 * @return OperationVo object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see Operation
 * @see OperationVo
 */
fun Operation.toVo() = OperationVo(
    id = this.id,
    name = this.name,
    code = this.code,
    scopeId = this.scopeId
)
