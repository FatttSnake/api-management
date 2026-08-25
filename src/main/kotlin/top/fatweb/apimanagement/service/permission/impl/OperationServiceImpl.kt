package top.fatweb.apimanagement.service.permission.impl

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl
import org.springframework.stereotype.Service
import top.fatweb.apimanagement.entity.permission.Operation
import top.fatweb.apimanagement.mapper.permission.OperationMapper
import top.fatweb.apimanagement.service.permission.IOperationService

/**
 * Operation service implement
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ServiceImpl
 * @see OperationMapper
 * @see Operation
 * @see IOperationService
 */
@Service
class OperationServiceImpl : ServiceImpl<OperationMapper, Operation>(), IOperationService
