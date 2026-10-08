package top.fatweb.apimanagement.mapper.api

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import top.fatweb.apimanagement.entity.api.ApiInterface

/**
 * API interface mapper
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see BaseMapper
 * @see ApiInterface
 */
@Mapper
interface ApiInterfaceMapper : BaseMapper<ApiInterface> {
    /**
     * Select the interfaces a caller may reach, filtered by the database
     *
     * Enabled interfaces of enabled plugins only - the plugin master switch is part of the
     * join because an interface's own `enable` does not say whether a request would be
     * admitted. A caller that is not [unrestricted] additionally sees only the open DEFAULT
     * interfaces plus those named in [codes], which is the same rule the access interceptor
     * applies per request.
     *
     * @param pluginIds Narrow to a set of plugins
     * @param codeIn Narrow to a set of API codes; the bound for "may these codes be held"
     * @param unrestricted Whether the caller is the super admin and sees every interface
     * @param codes Operation codes the caller holds; ignored when [unrestricted]
     * @return List of ApiInterface object ordered by name, then ID
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiInterface
     */
    fun selectSelectableList(
        @Param("pluginIds") pluginIds: Collection<String>?,
        @Param("codeIn") codeIn: Collection<String>?,
        @Param("unrestricted") unrestricted: Boolean,
        @Param("codes") codes: Collection<String>?
    ): List<ApiInterface>
}
