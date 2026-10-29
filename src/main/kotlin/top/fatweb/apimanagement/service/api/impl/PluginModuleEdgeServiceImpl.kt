package top.fatweb.apimanagement.service.api.impl

import com.baomidou.dynamic.datasource.annotation.DS
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl
import org.springframework.stereotype.Service
import top.fatweb.apimanagement.entity.api.PluginModuleEdge
import top.fatweb.apimanagement.mapper.api.PluginModuleEdgeMapper
import top.fatweb.apimanagement.service.api.IPluginModuleEdgeService

/**
 * Plugin module edge service implement.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Service
@DS("master")
class PluginModuleEdgeServiceImpl :
    ServiceImpl<PluginModuleEdgeMapper, PluginModuleEdge>(),
    IPluginModuleEdgeService
