package top.fatweb.apimanagement.mapper.system

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import org.apache.ibatis.annotations.Mapper
import top.fatweb.apimanagement.entity.system.ApiInterface

/**
 * API interface mapper
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see BaseMapper
 * @see ApiInterface
 */
@Mapper
interface ApiInterfaceMapper : BaseMapper<ApiInterface>
