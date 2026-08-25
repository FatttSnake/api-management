package top.fatweb.apimanagement.vo.system

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/**
 * Software information value object
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Schema(description = "软件信息返回参数")
data class SoftwareInfoVo(
    /**
     * Service version
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "服务版本")
    val serviceVersion: String,

    /**
     * Operating system
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "操作系统")
    val os: String,

    /**
     * Bitness (32 or 64) of the operating system.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "位数")
    val bitness: Int,

    /**
     * Version of Java
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "Java 版本")
    val javaVersion: String,

    /**
     * Version date of Java
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "Java 版本日期")
    val javaVersionDate: String,

    /**
     * Vendor of Java
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "Java 供应商")
    val javaVendor: String,

    /**
     * Name of Java runtime
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Deprecated("Will be removed in the next minor version")
    @field:Schema(description = "Java 运行时")
    val javaRuntime: String,

    /**
     * Version of Java runtime
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Deprecated("Will be removed in the next minor version")
    @field:Schema(description = "Java 运行时版本")
    val javaRuntimeVersion: String,

    /**
     * Name of Java virtual machine
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "Java 虚拟机")
    val jvm: String,

    /**
     * Version of Java virtual machine
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "Java 虚拟机版本")
    val jvmVersion: String,

    /**
     * Version of Java Virtual machine
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "Java 虚拟机信息")
    val jvmInfo: String,

    /**
     * Vendor of Java Virtual machine
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "Java 虚拟机供应商")
    val jvmVendor: String,

    /**
     * Version of Java class
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "Java 字节文件版本")
    val javaClassVersion: String,

    /**
     * Boot time of operating system
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "操作系统启动时间")
    val osBootTime: LocalDateTime,

    /**
     * Startup time of server
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(description = "服务器启动时间")
    val serverStartupTime: LocalDateTime
)
