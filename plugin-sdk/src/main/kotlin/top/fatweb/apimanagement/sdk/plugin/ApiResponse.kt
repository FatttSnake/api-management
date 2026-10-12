package top.fatweb.apimanagement.sdk.plugin

import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable

/**
 * Plugin API response
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Schema(description = "插件 API 响应参数")
data class ApiResponse<T>(
    /**
     * Business code; 0 means success
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Schema(description = "业务码，0 表示成功", example = "0")
    val code: Int,

    /**
     * Whether the call succeeded
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Schema(description = "是否调用成功")
    val success: Boolean,

    /**
     * Response message
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Schema(description = "信息")
    val msg: String,

    /**
     * Response data
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Schema(description = "数据")
    val data: T?
) : Serializable {
    companion object {
        /**
         * Success business code
         */
        const val CODE_SUCCESS = 0

        /**
         * Build a successful response
         *
         * @param data Response data
         * @param msg Response message
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        fun <T> ok(data: T? = null, msg: String = "success") = ApiResponse(CODE_SUCCESS, true, msg, data)

        /**
         * Build a failed response
         *
         * @param code Business error code
         * @param msg Error message
         * @param data Optional payload
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        fun <T> fail(code: Int, msg: String, data: T? = null) = ApiResponse(code, false, msg, data)
    }
}
