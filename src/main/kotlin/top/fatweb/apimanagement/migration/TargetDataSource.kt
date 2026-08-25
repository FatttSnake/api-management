package top.fatweb.apimanagement.migration

/**
 * Target migrate data source
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class TargetDataSource(
    val value: String
)
