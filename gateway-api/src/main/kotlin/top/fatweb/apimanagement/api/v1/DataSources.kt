package top.fatweb.apimanagement.api.v1

import javax.sql.DataSource

/**
 * Plugin-scoped datasources.
 *
 * A declared SQLite datasource is present as soon as the plugin starts. A MySQL datasource
 * appears only after an administrator configures it.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
interface DataSources {
    /**
     * Returns configured datasource names.
     *
     * @return immutable name set
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun names(): Set<String>

    /**
     * Returns all configured datasources.
     *
     * @return immutable datasource map
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun all(): Map<String, DataSource>

    /**
     * Returns one datasource.
     *
     * @param name datasource name
     * @return datasource, or null when not configured
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun get(name: String): DataSource?
}
