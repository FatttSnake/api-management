package top.fatweb.apimanagement.service.permission.impl

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl
import org.springframework.stereotype.Service
import top.fatweb.apimanagement.entity.permission.Menu
import top.fatweb.apimanagement.mapper.permission.MenuMapper
import top.fatweb.apimanagement.service.permission.IMenuService

/**
 * Menu service implement
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ServiceImpl
 * @see MenuMapper
 * @see Menu
 * @see IMenuService
 */
@Service
class MenuServiceImpl : ServiceImpl<MenuMapper, Menu>(), IMenuService
