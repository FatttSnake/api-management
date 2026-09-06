package top.fatweb.apimanagement.converter.api

import com.baomidou.mybatisplus.core.metadata.IPage
import top.fatweb.apimanagement.entity.api.ApiAccount
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.api.ApiAccountVo
import top.fatweb.apimanagement.vo.permission.UserWithInfoVo

/**
 * Convert to ApiAccountVo object
 *
 * @return ApiAccountVo object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ApiAccount
 * @see ApiAccountVo
 */
fun ApiAccount.toVo(resolveUserInfo: ((id: Long) -> UserWithInfoVo?)? = null) = ApiAccountVo(
    id = this.id,
    userId = this.userId,
    balance = this.balance,
    enable = this.enable?.let { it == 1 },
    createTime = this.createTime,
    updateTime = this.updateTime,
    userVo = this.userId?.let { userId -> resolveUserInfo?.invoke(userId) }
)

/**
 * Convert to PageVo<ApiAccount> object
 *
 * @return PageVo<ApiAccount> object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see IPage
 * @see ApiAccount
 * @see PageVo
 */
fun IPage<ApiAccount>.toVoPage(resolveUserInfo: ((id: Long) -> UserWithInfoVo?)? = null) = PageVo(
    total = this.total,
    pages = this.pages,
    size = this.size,
    current = this.current,
    records = this.records.map { it.toVo(resolveUserInfo) }
)
