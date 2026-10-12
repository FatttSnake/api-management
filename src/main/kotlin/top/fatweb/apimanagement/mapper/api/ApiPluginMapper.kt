package top.fatweb.apimanagement.mapper.api

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import com.baomidou.mybatisplus.core.metadata.IPage
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import top.fatweb.apimanagement.entity.api.ApiPlugin

/**
 * API plugin mapper
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Mapper
interface ApiPluginMapper : BaseMapper<ApiPlugin> {
    /**
     * Select one page of the plugins a caller may reach
     *
     * A plugin qualifies when it is enabled and owns at least one interface the caller may call.
     *
     * @param page Pagination
     * @param searchName Plugin name to search for
     * @param unrestricted Whether the caller is the super admin and reaches every interface
     * @param codes Operation codes the caller holds; ignored when [unrestricted]
     * @return ApiPlugin in page, ordered by create time descending
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun selectCallablePage(
        page: IPage<ApiPlugin>,
        @Param("searchName") searchName: String?,
        @Param("unrestricted") unrestricted: Boolean,
        @Param("codes") codes: Collection<String>?
    ): IPage<ApiPlugin>
}
