package top.fatweb.apimanagement.entity.common

import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable

/**
 * Response result entity
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
class ResponseResult<T> private constructor(
    @field:Schema(description = "响应码", defaultValue = "200")
    val code: Int,

    @field:Schema(description = "是否调用成功")
    val success: Boolean,

    @field:Schema(description = "信息")
    val msg: String,

    @field:Schema(description = "数据")
    val data: T?
) : Serializable {
    companion object {
        /**
         * Build response result object
         *
         * @param code Response code
         * @param success Is successful
         * @param msg Response message
         * @param data Response data
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        fun <T> build(code: Int, success: Boolean, msg: String, data: T?) =
            ResponseResult(code, success, msg, data)

        /**
         * Build response result object
         *
         * @param code Response code object
         * @param success Is successful
         * @param msg Response message
         * @param data Response data
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         * @see ResponseCode
         */
        fun <T> build(code: ResponseCode, success: Boolean, msg: String, data: T?) =
            build(code.code, success, msg, data)

        /**
         * Build successful response result object
         *
         * @param code Response code object
         * @param msg Response message
         * @param data Response data
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         * @see ResponseCode
         */
        fun <T> success(code: ResponseCode = ResponseCode.SYSTEM_OK, msg: String = "success", data: T? = null) =
            build(code, true, msg, data)

        /**
         * Build failure response result object
         *
         * @param code Response code object
         * @param msg Response message
         * @param data Response data
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         * @see ResponseCode
         */
        fun <T> fail(code: ResponseCode = ResponseCode.SYSTEM_ERROR, msg: String = "fail", data: T? = null) =
            build(code, false, msg, data)

        /**
         * Build database successful response result object
         *
         * @param code Response code object
         * @param msg Response message
         * @param data Response data
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         * @see ResponseCode
         */
        fun <T> databaseSuccess(
            code: ResponseCode = ResponseCode.DATABASE_SELECT_SUCCESS, msg: String = "success", data: T? = null
        ) = build(code, true, msg, data)

        /**
         * Build database failure response result object
         *
         * @param code Response code object
         * @param msg Response message
         * @param data Response data
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         * @see ResponseCode
         */
        fun <T> databaseFail(
            code: ResponseCode = ResponseCode.DATABASE_SELECT_FAILED, msg: String = "fail", data: T? = null
        ) = build(code, false, msg, data)
    }
}
