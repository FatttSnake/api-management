package top.fatweb.apimanagement.vo.api

import io.swagger.v3.oas.annotations.media.Schema

/**
 * One dependency module of a fetch task.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Schema(description = "依赖模块获取进度")
data class PluginModuleFetchModuleVo(
    /**
     * Maven group
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "Maven group")
    val groupId: String,
    /**
     * Maven artifact
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "Maven artifact")
    val artifactId: String,
    /**
     * Maven version
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "Maven version")
    val moduleVersion: String,
    /**
     * Expected SHA-256 digest
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "模块 SHA-256")
    val sha256: String,
    /**
     * Expected size in bytes
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "模块大小（字节）")
    val size: Long,
    /**
     * Bytes downloaded so far
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "已下载字节数")
    val downloadedBytes: Long,
    /**
     * Download progress from 0 to 100
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "下载进度")
    val percent: Int,
    /**
     * Module status
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "模块状态")
    val status: PluginModuleFetchModuleStatus,
    /**
     * Failure message
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "失败原因")
    val message: String?,
)
