package top.fatweb.apimanagement.vo.api

import io.swagger.v3.oas.annotations.media.Schema
import tools.jackson.databind.annotation.JsonSerialize
import tools.jackson.databind.ser.std.ToStringSerializer
import java.time.LocalDateTime

/**
 * Plugin dependency module value object.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Schema(description = "API 插件依赖模块返回参数")
data class PluginModuleVo(
    /**
     * ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:JsonSerialize(using = ToStringSerializer::class)
    val id: Long?,
    /**
     * Maven group
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val groupId: String?,
    /**
     * Maven artifact
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val artifactId: String?,
    /**
     * Resolved Maven version
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val moduleVersion: String?,
    /**
     * SHA-256 digest
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val sha256: String?,
    /**
     * Size in bytes
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val size: Long?,
    /**
     * Source repository
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val source: String?,
    /**
     * Install time
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val installTime: LocalDateTime?,
    /**
     * Create time
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val createTime: LocalDateTime?,
    /**
     * Update time
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val updateTime: LocalDateTime?,
    /**
     * Whether any plugin currently references the module
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "是否被插件引用")
    val referenced: Boolean,
    /**
     * Plugins that currently reference the module
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "引用该模块的插件")
    val referencedBy: List<PluginModuleReferenceVo>,
)
