package top.fatweb.apimanagement.param.system.apiKey

import io.swagger.v3.oas.annotations.media.Schema
import top.fatweb.apimanagement.annotation.ParamProcessor
import top.fatweb.apimanagement.param.PageSortParam

/**
 * Get API key parameters
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see PageSortParam
 */
@ParamProcessor
@Schema(description = "API Key 查询请求参数")
data class ApiKeyGetParam(
    /**
     * Key name to search for
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "查询 Key 名称", example = "my-key")
    var searchName: String?,

    /**
     * Enabled status
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "状态", allowableValues = ["true", "false"], example = "true")
    var status: Boolean?,

    /**
     * Owner user ID (admin only)
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "所属用户 ID（管理员查询指定用户）")
    var userId: Long?
) : PageSortParam()
