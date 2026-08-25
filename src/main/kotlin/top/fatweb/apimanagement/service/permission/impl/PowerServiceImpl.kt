package top.fatweb.apimanagement.service.permission.impl

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl
import org.springframework.stereotype.Service
import top.fatweb.apimanagement.converter.permission.toVo
import top.fatweb.apimanagement.entity.permission.Power
import top.fatweb.apimanagement.entity.permission.PowerSet
import top.fatweb.apimanagement.mapper.permission.PowerMapper
import top.fatweb.apimanagement.service.permission.*

/**
 * Power service implement
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see IModuleService
 * @see IMenuService
 * @see IScopeService
 * @see IOperationService
 * @see ServiceImpl
 * @see PowerMapper
 * @see Power
 * @see IPowerService
 */
@Service
class PowerServiceImpl(
    private val moduleService: IModuleService,
    private val menuService: IMenuService,
    private val scopeService: IScopeService,
    private val operationService: IOperationService
) : ServiceImpl<PowerMapper, Power>(), IPowerService {
    override fun getList() = PowerSet().apply {
        moduleList = moduleService.list()
        menuList = menuService.list()
        scopeLists = scopeService.list()
        operationList = operationService.list()
    }.toVo()
}
