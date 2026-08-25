package top.fatweb.apimanagement.service.permission.impl

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl
import org.springframework.stereotype.Service
import top.fatweb.apimanagement.entity.permission.UserInfo
import top.fatweb.apimanagement.mapper.permission.UserInfoMapper
import top.fatweb.apimanagement.service.permission.IUserInfoService

/**
 * User information service implement
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ServiceImpl
 * @see UserInfoMapper
 * @see UserInfo
 * @see IUserInfoService
 */
@Service
class UserInfoServiceImpl : ServiceImpl<UserInfoMapper, UserInfo>(), IUserInfoService
