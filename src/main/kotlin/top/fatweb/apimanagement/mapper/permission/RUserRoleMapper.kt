package top.fatweb.apimanagement.mapper.permission

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import org.apache.ibatis.annotations.Mapper
import top.fatweb.apimanagement.entity.permission.RUserRole

/**
 * User role intermediate mapper
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see BaseMapper
 * @see RUserRole
 */
@Mapper
interface RUserRoleMapper : BaseMapper<RUserRole>
