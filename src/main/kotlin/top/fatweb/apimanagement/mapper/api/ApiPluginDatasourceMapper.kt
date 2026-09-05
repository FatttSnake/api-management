package top.fatweb.apimanagement.mapper.api

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import org.apache.ibatis.annotations.Mapper
import top.fatweb.apimanagement.entity.api.ApiPluginDatasource

/**
 * Plugin datasource mapper
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see BaseMapper
 * @see ApiPluginDatasource
 */
@Mapper
interface ApiPluginDatasourceMapper : BaseMapper<ApiPluginDatasource>
