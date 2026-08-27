package top.fatweb.apimanagement.mapper.system

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import org.apache.ibatis.annotations.Mapper
import top.fatweb.apimanagement.entity.system.ApiKey

/**
 * API key mapper
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see BaseMapper
 * @see ApiKey
 */
@Mapper
interface ApiKeyMapper : BaseMapper<ApiKey>
