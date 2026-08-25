package top.fatweb.apimanagement.param.system

import io.swagger.v3.oas.annotations.media.Schema

/**
 * Update sensitive word settings parameters
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Schema(defaultValue = "敏感词修改请求参数")
data class SensitiveWordUpdateParam(
    @field:Schema(description = "ID 列表")
    val ids: Set<Long> = emptySet()
)
