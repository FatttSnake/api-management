package top.fatweb.apimanagement.vo.api

import io.swagger.v3.oas.annotations.media.Schema

/**
 * Available API interface value object
 *
 * The two fields the key-authorization picker actually reads: [code] is what a key's
 * permissions store and [name] is the option label.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Schema(description = "可授权 API 接口返回参数")
data class ApiKeyAvailableInterfaceVo(
    /**
     * API scoping code
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Schema(description = "API 编码", example = "api:echo:v1:ping")
    val code: String?,

    /**
     * API name
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Schema(description = "API 名称", example = "getRandom")
    val name: String?
)
