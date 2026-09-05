package top.fatweb.apimanagement.mapper.api

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import org.apache.ibatis.annotations.Mapper
import top.fatweb.apimanagement.entity.api.ApiUsage

/**
 * API usage mapper
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see BaseMapper
 * @see ApiUsage
 */
@Mapper
interface ApiUsageMapper : BaseMapper<ApiUsage>
