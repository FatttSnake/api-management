package top.fatweb.apimanagement.vo.permission

import io.swagger.v3.oas.annotations.media.Schema

/**
 * Token value object
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Schema(description = "Token 返回参数")
data class TokenVo(
    /**
     * Refresh Token
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(
        description = "Refresh Token",
        example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJqdGkiOiJkYTllYjFkYmVmZDQ0OWRkOThlOGNjNzZlNzZkMDgyNSIsInN1YiI6IjE3MDk5ODYwNTg2Nzk5NzU5MzgiLCJpc3MiOiJGYXRXZWIiLCJpYXQiOjE2OTY1MjgxMTcsImV4cCI6MTY5NjUzNTMxN30.U2ZsyrGk7NbsP-DJfdz9xgWSfect5r2iKQnlEsscAA8"
    )
    val refreshToken: String,

    /**
     * Access Token
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(
        description = "Access Token",
        example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJqdGkiOiJkYTllYjFkYmVmZDQ0OWRkOThlOGNjNzZlNzZkMDgyNSIsInN1YiI6IjE3MDk5ODYwNTg2Nzk5NzU5MzgiLCJpc3MiOiJGYXRXZWIiLCJpYXQiOjE2OTY1MjgxMTcsImV4cCI6MTY5NjUzNTMxN30.U2ZsyrGk7NbsP-DJfdz9xgWSfect5r2iKQnlEsscAA8"
    )
    val accessToken: String,

    /**
     * CSRF Token
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(
        description = "CSRF Token",
        example = "a1b2c3d4e5f6g7h8"
    )
    val csrfToken: String
)
