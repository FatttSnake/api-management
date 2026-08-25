package top.fatweb.apimanagement.mapper.permission

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import org.apache.ibatis.annotations.Mapper
import top.fatweb.apimanagement.entity.permission.Scope

/**
 * Scope mapper
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see BaseMapper
 * @see Scope
 */
@Mapper
interface ScopeMapper : BaseMapper<Scope>
