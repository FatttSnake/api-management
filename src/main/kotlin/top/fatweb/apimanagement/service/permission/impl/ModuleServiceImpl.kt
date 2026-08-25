package top.fatweb.apimanagement.service.permission.impl

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl
import org.springframework.stereotype.Service
import top.fatweb.apimanagement.entity.permission.Module
import top.fatweb.apimanagement.mapper.permission.ModuleMapper
import top.fatweb.apimanagement.service.permission.IModuleService

/**
 * Module service implement
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ServiceImpl
 * @see ModuleMapper
 * @see Module
 * @see IModuleService
 */
@Service
class ModuleServiceImpl : ServiceImpl<ModuleMapper, Module>(), IModuleService
