package top.fatweb.apimanagement.converter.api

import top.fatweb.apimanagement.entity.api.ApiAccount
import top.fatweb.apimanagement.vo.api.ApiAccountVo

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
    enable = this.enable?.let { it == 1 },
    createTime = this.createTime,
    updateTime = this.updateTime
)
