package top.fatweb.apimanagement.param.system.api

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import top.fatweb.apimanagement.annotation.ParamProcessor

/**
 * Test API plugin datasource parameters
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@ParamProcessor
@Schema(description = "API 插件数据源测试请求参数")
data class ApiPluginDatasourceTestParam(
    /**
     * Datasource name, as the plugin declares it
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "数据源名称", required = true, example = "main")
    @field:NotBlank(message = "Datasource name can not be blank")
    var name: String?,
    /**
     * Values to test with, leaving one out to test with what is stored
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "要测试的配置项（缺省的项用已存值）")
    @field:Valid
    var values: List<ApiPluginConfigValueParam>?,
)
