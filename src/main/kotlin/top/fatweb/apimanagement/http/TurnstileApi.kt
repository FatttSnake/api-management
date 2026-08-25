package top.fatweb.apimanagement.http

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import top.fatweb.apimanagement.http.entity.turnstile.SiteverifyResponse

/**
 * Turnstile http request api
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
interface TurnstileApi {
    /**
     * Turnstile post verify captcha code
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see SiteverifyResponse
     */
    @FormUrlEncoded
    @POST("siteverify")
    suspend fun siteverify(
        @Field("response") captchaCode: String,
        @Field("secret") secret: String
    ): SiteverifyResponse
}
