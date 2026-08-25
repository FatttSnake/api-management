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
import top.fatweb.apimanagement.filter.JwtAuthenticationTokenFilter
import top.fatweb.apimanagement.handler.JwtAccessDeniedHandler
import top.fatweb.apimanagement.handler.JwtAuthenticationEntryPointHandler

/**
 * Spring Security configuration
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see JwtAuthenticationTokenFilter
 * @see JwtAuthenticationEntryPointHandler
 * @see JwtAccessDeniedHandler
 */
@Configuration
@EnableMethodSecurity
class SecurityConfig(
    private val jwtAuthenticationTokenFilter: JwtAuthenticationTokenFilter,
    private val authenticationEntryPointHandler: JwtAuthenticationEntryPointHandler,
    private val accessDeniedHandler: JwtAccessDeniedHandler
) {
    @Bean
    fun passwordEncoder(): PasswordEncoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()

    @Bean
    fun authenticationManager(authenticationConfiguration: AuthenticationConfiguration): AuthenticationManager =
        authenticationConfiguration.authenticationManager

    @Bean
    fun corsConfigurationSource(): UrlBasedCorsConfigurationSource {
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", CorsConfiguration().apply {
                allowedOriginPatterns = listOf("*")
                allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                allowedHeaders =
                    listOf("Authorization", "Content-Type", "X-CSRF-TOKEN", "X-Requested-With", "Cache-Control")
                allowCredentials = true
                maxAge = 3600L
                exposedHeaders = listOf("X-CSRF-TOKEN")
            })
        }
    }

    @Bean
    fun securityFilterChain(httpSecurity: HttpSecurity): SecurityFilterChain = httpSecurity
        .cors {
            it.configurationSource(
                corsConfigurationSource()
            )
        }

        .csrf { it.disable() }

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
                    "/config"
                ).anonymous()
                .requestMatchers(
                    "/token",
                    "/tool/dist/**",
                    "/tool/base/**",
                    "/tool/store",
                    "/tool/store/*",
                    "/system/user/info/*"
                ).permitAll()
                .anyRequest().authenticated()
        }

        .logout {
            it.disable()
        }

        .exceptionHandling {
            it.authenticationEntryPoint(
                authenticationEntryPointHandler
            )
            it.accessDeniedHandler(
                accessDeniedHandler
            )
        }

        .addFilterBefore(jwtAuthenticationTokenFilter, UsernamePasswordAuthenticationFilter::class.java).build()
}
