package top.fatweb.apimanagement.config

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource
import jakarta.annotation.PostConstruct
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.migration.JavaMigration
import org.springframework.context.annotation.Configuration
import top.fatweb.apimanagement.migration.TargetDataSource
import top.fatweb.apimanagement.properties.FlywayProperties
import javax.sql.DataSource

/**
 * Flyway configuration
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see FlywayProperties
 * @see DataSource
 * @see JavaMigration
 */
@Configuration
class FlywayConfig(
    private val flywayProperties: FlywayProperties,
    private val dataSource: DataSource,
    private val migrations: List<JavaMigration>
) {
    @PostConstruct
    fun migrateOrder() {
        val ds = dataSource as DynamicRoutingDataSource
        ds.dataSources.forEach { (k: String, v: DataSource?) ->
            val javaMigrations = migrations.filter {
                val annotation = it::class.java.getAnnotation(TargetDataSource::class.java)
                annotation?.value == k
            }

            val flyway = Flyway.configure()
                .dataSource(v)
                .locations(*flywayProperties.locations.map { "$it/$k" }.toTypedArray())
                .javaMigrations(*javaMigrations.toTypedArray())
                .baselineOnMigrate(flywayProperties.baselineOnMigrate)
                .table(flywayProperties.table)
                .outOfOrder(flywayProperties.outOfOrder)
                .validateOnMigrate(flywayProperties.validateOnMigrate)
                .encoding(flywayProperties.encoding)
                .sqlMigrationPrefix(flywayProperties.sqlMigrationPrefix)
                .sqlMigrationSeparator(flywayProperties.sqlMigrationSeparator)
                .sqlMigrationSuffixes(*flywayProperties.sqlMigrationSuffixes.toTypedArray())
                .baselineVersion(flywayProperties.baselineVersion)
                .load()
            flyway.migrate()
        }
    }
}
