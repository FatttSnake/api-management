package top.fatweb.apimanagement.param.system.apiKey

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty

/**
 * Delete API key parameters
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Schema(description = "API Key 删除请求参数")
data class ApiKeyDeleteParam(
    /**
     * List of API key IDs
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "Key ID 列表", required = true)
    @field:NotEmpty(message = "IDs can not be empty")
    var ids: List<Long>?
)
