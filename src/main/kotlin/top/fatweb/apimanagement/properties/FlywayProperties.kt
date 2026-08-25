package top.fatweb.apimanagement.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import java.nio.charset.Charset

/**
 * Flyway properties
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@ConfigurationProperties("spring.flyway")
data class FlywayProperties(
    /**
     * Locations of migrations scripts. Can contain the special "{vendor}" placeholder to
     * use vendor-specific locations.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val locations: List<String> = listOf("classpath:db/migration"),

    /**
     * Name of the schema history table that will be used by Flyway.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val table: String = "flyway_schema_history",

    /**
     * Whether to allow migrations to be run out of order.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val outOfOrder: Boolean = false,

    /**
     * Whether to automatically call validate when performing a migration.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val validateOnMigrate: Boolean = true,

    /**
     * Encoding of SQL migrations.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val encoding: Charset = Charsets.UTF_8,

    /**
     * File name prefix for SQL migrations.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val sqlMigrationPrefix: String = "V",

    /**
     * File name separator for SQL migrations.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val sqlMigrationSeparator: String = "__",

    /**
     * File name suffix for SQL migrations.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val sqlMigrationSuffixes: List<String> = listOf(".sql"),

    /**
     * Whether to automatically call baseline when migrating a non-empty schema.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val baselineOnMigrate: Boolean = true,

    /**
     * Version to tag an existing schema with when executing baseline.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    val baselineVersion: String = "0"
)
