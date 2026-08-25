package top.fatweb.apimanagement.service.permission.impl

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl
import org.springframework.stereotype.Service
import top.fatweb.apimanagement.entity.permission.RUserRole
import top.fatweb.apimanagement.mapper.permission.RUserRoleMapper
import top.fatweb.apimanagement.service.permission.IRUserRoleService

/**
 * User role intermediate service implement
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ServiceImpl
 * @see RUserRoleMapper
 * @see RUserRole
 * @see IRUserRoleService
 */
@Service
class RUserRoleServiceImpl : ServiceImpl<RUserRoleMapper, RUserRole>(), IRUserRoleService
