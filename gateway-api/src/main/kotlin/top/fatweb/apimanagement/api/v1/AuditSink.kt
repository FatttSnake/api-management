package top.fatweb.apimanagement.api.v1

/**
 * Writes plugin events to the platform audit log.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
interface AuditSink {
    /**
     * Records an audit event.
     *
     * @param event event
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun record(event: AuditEvent)
}
