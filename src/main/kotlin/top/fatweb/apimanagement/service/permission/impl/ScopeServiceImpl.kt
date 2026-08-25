package top.fatweb.apimanagement.service.permission.impl

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl
import org.springframework.stereotype.Service
import top.fatweb.apimanagement.entity.permission.Scope
import top.fatweb.apimanagement.mapper.permission.ScopeMapper
import top.fatweb.apimanagement.service.permission.IScopeService

/**
 * Scope service implement
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ServiceImpl
 * @see ScopeMapper
 * @see Scope
 * @see IScopeService
 */
@Service
class ScopeServiceImpl : ServiceImpl<ScopeMapper, Scope>(), IScopeService
