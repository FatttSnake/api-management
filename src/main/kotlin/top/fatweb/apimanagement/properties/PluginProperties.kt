package top.fatweb.apimanagement.properties

import jakarta.validation.Valid
import jakarta.validation.constraints.AssertTrue
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import java.net.URI
import java.time.Duration

/**
 * Plugin properties
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Validated
@ConfigurationProperties("app.plugin")
data class PluginProperties(
    /**
     * Module properties
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ModuleProperties
     */
    @field:Valid val module: ModuleProperties = ModuleProperties(),
) {
    /**
     * Dependency module properties
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    data class ModuleProperties(
        /**
         * Whether missing modules may be downloaded from the configured Maven repositories
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        val online: Boolean = true,
        /**
         * Maven repositories tried in order, only these are used
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        val repositories: List<String> = DEFAULT_REPOSITORIES,
        /**
         * Connect timeout of one download
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        val connectTimeout: Duration = Duration.ofSeconds(10),
        /**
         * Timeout of one module download
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        val requestTimeout: Duration = Duration.ofMinutes(5),
    ) {
        /**
         * Check repository properties
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        @AssertTrue(message = "Module repositories must be absolute http(s) URLs")
        fun isRepositories(): Boolean = !online ||
            (repositories.isNotEmpty() && repositories.all(::isRepository))

        private fun isRepository(value: String): Boolean = runCatching {
            val uri = URI(value)
            (uri.scheme == "http" || uri.scheme == "https") && !uri.host.isNullOrBlank()
        }.getOrDefault(false)
    }

    companion object {
        /**
         * Default module repositories
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        val DEFAULT_REPOSITORIES: List<String> = listOf("https://repo.maven.apache.org/maven2/")
    }
}
