package top.fatweb.apimanagement.service.permission.impl

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl
import org.springframework.stereotype.Service
import top.fatweb.apimanagement.entity.permission.RPowerRole
import top.fatweb.apimanagement.mapper.permission.RPowerRoleMapper
import top.fatweb.apimanagement.service.permission.IRPowerRoleService

/**
 * Power role intermediate service implement
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ServiceImpl
 * @see RPowerRoleMapper
 * @see RPowerRole
 * @see IRPowerRoleService
 */
@Service
class RPowerRoleServiceImpl : ServiceImpl<RPowerRoleMapper, RPowerRole>(), IRPowerRoleService
