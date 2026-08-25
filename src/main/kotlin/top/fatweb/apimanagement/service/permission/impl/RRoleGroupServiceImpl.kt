package top.fatweb.apimanagement.service.permission.impl

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl
import org.springframework.stereotype.Service
import top.fatweb.apimanagement.entity.permission.RRoleGroup
import top.fatweb.apimanagement.mapper.permission.RRoleGroupMapper
import top.fatweb.apimanagement.service.permission.IRRoleGroupService

/**
 * Role group intermediate service implement
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ServiceImpl
 * @see RRoleGroupMapper
 * @see RRoleGroup
 * @see IRRoleGroupService
 */
@Service
class RRoleGroupServiceImpl : ServiceImpl<RRoleGroupMapper, RRoleGroup>(), IRRoleGroupService
