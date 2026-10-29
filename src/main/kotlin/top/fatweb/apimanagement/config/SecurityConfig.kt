package top.fatweb.apimanagement.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import top.fatweb.apimanagement.component.storage.StorageRoutes
import top.fatweb.apimanagement.filter.ApiKeyAuthenticationTokenFilter
import top.fatweb.apimanagement.filter.JwtAuthenticationTokenFilter
import top.fatweb.apimanagement.handler.JwtAccessDeniedHandler
import top.fatweb.apimanagement.handler.JwtAuthenticationEntryPointHandler
import top.fatweb.apimanagement.properties.ServerProperties
import top.fatweb.apimanagement.util.TimezoneUtil

/**
 * Spring Security configuration
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Configuration
@EnableMethodSecurity
class SecurityConfig(
    private val serverProperties: ServerProperties,
    private val apiKeyAuthenticationTokenFilter: ApiKeyAuthenticationTokenFilter,
    private val jwtAuthenticationTokenFilter: JwtAuthenticationTokenFilter,
    private val authenticationEntryPointHandler: JwtAuthenticationEntryPointHandler,
    private val accessDeniedHandler: JwtAccessDeniedHandler,
) {
    @Bean
    fun passwordEncoder(): PasswordEncoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()

    @Bean
    fun authenticationManager(authenticationConfiguration: AuthenticationConfiguration): AuthenticationManager =
        authenticationConfiguration.authenticationManager

    @Bean
    fun corsConfigurationSource(): UrlBasedCorsConfigurationSource = UrlBasedCorsConfigurationSource().apply {
        registerCorsConfiguration(
            "/**",
            CorsConfiguration().apply {
                // Credentials are sent with the request (the refresh token is a cookie), so the
                // origin has to be a configured one rather than any site that asks
                allowedOriginPatterns = serverProperties.security.allowedOriginPatterns
                allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                allowedHeaders =
                    listOf(
                        "Authorization",
                        "Content-Type",
                        "X-CSRF-TOKEN",
                        "X-Requested-With",
                        "Cache-Control",
                        TimezoneUtil.TIMEZONE_OFFSET_HEADER,
                    )
                allowCredentials = true
                maxAge = 3600L
                exposedHeaders = listOf("X-CSRF-TOKEN")
            },
        )
    }

    @Bean
    fun securityFilterChain(httpSecurity: HttpSecurity): SecurityFilterChain = httpSecurity
        .cors {
            it.configurationSource(
                corsConfigurationSource(),
            )
        }.csrf { it.disable() }
        .authorizeHttpRequests {
            it
                .requestMatchers(
                    "/error/thrown",
                    "/doc.html",
                    "/swagger-ui/**",
                    "/webjars/**",
                    "/v3/**",
                    "/swagger-ui.html",
                    "/favicon.ico",
                    "/login",
                    "/register",
                    "/forget",
                    "/retrieve",
                    "/health",
                    "/config",
                ).anonymous()
                .requestMatchers(
                    "/token",
                    "/tool/dist/**",
                    "/tool/base/**",
                    "/tool/store",
                    "/tool/store/*",
                    StorageRoutes.PUBLIC_STORAGE_MATCHER,
                    "/api/**",
                ).permitAll()
                .anyRequest()
                .authenticated()
        }.logout {
            it.disable()
        }.exceptionHandling {
            it.authenticationEntryPoint(
                authenticationEntryPointHandler,
            )
            it.accessDeniedHandler(
                accessDeniedHandler,
            )
        }.addFilterBefore(apiKeyAuthenticationTokenFilter, UsernamePasswordAuthenticationFilter::class.java)
        .addFilterBefore(jwtAuthenticationTokenFilter, UsernamePasswordAuthenticationFilter::class.java)
        .build()
}
