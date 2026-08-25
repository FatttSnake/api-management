package top.fatweb.apimanagement.service.permission.impl

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl
import org.springframework.stereotype.Service
import top.fatweb.apimanagement.entity.permission.RUserGroup
import top.fatweb.apimanagement.mapper.permission.RUserGroupMapper
import top.fatweb.apimanagement.service.permission.IRUserGroupService

/**
 * User group intermediate service implement
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ServiceImpl
 * @see RUserGroupMapper
 * @see RUserGroup
 * @see IRUserGroupService
 */
@Service
class RUserGroupServiceImpl : ServiceImpl<RUserGroupMapper, RUserGroup>(), IRUserGroupService
