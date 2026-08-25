package top.fatweb.apimanagement.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import retrofit2.Retrofit
import top.fatweb.apimanagement.http.TurnstileApi

/**
 * Retrofit configuration
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Configuration
class RetrofitConfig {
    @Bean
    fun turnstileApi(
        jacksonConverterFactory: Jackson3ConverterFactory
    ): TurnstileApi {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://challenges.cloudflare.com/turnstile/v0/")
            .addConverterFactory(jacksonConverterFactory)
            .build()

        return retrofit.create(TurnstileApi::class.java)
    }
}
