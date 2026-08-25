package top.fatweb.apimanagement.param

import io.swagger.v3.oas.annotations.media.Schema

/**
 * Captcha code parameter
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
open class CaptchaCodeParam {
    /**
     * Captcha code
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Schema(description = "验证码", required = true)
    var captchaCode: String? = null
}
