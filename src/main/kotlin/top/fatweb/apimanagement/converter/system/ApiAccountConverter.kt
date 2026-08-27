package top.fatweb.apimanagement.converter.system

import top.fatweb.apimanagement.entity.system.ApiAccount
import top.fatweb.apimanagement.vo.system.ApiAccountVo

/**
 * Convert to ApiAccountVo object
 *
 * @return ApiAccountVo object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ApiAccount
 * @see ApiAccountVo
 */
fun ApiAccount.toVo() = ApiAccountVo(
    id = this.id,
    userId = this.userId,
    balance = this.balance,
    status = this.status?.let { it == 1 },
    createTime = this.createTime,
    updateTime = this.updateTime
)
