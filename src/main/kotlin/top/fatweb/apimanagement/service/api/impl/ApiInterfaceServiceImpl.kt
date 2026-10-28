package top.fatweb.apimanagement.service.api.impl

import com.baomidou.dynamic.datasource.annotation.DS
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl
import org.springframework.stereotype.Service
import top.fatweb.apimanagement.entity.api.ApiInterface
import top.fatweb.apimanagement.mapper.api.ApiInterfaceMapper
import top.fatweb.apimanagement.service.api.IApiInterfaceService

/**
 * API interface service implement.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Service
@DS("master")
class ApiInterfaceServiceImpl : ServiceImpl<ApiInterfaceMapper, ApiInterface>(), IApiInterfaceService
