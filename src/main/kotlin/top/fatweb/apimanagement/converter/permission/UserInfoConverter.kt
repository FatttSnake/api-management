package top.fatweb.apimanagement.converter.permission

import top.fatweb.apimanagement.entity.permission.UserInfo
import top.fatweb.apimanagement.vo.permission.base.UserInfoVo

/**
 * Convert to UserInfoVo object
 *
 * @return UserInfoVo object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see UserInfo
 * @see UserInfoVo
 */
fun UserInfo.toVo() = UserInfoVo(
    id = this.id,
    userId = this.userId,
    nickname = this.nickname,
    avatar = this.avatar,
    email = this.email,
    createTime = this.createTime,
    updateTime = this.updateTime
)
