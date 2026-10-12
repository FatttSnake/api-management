package top.fatweb.apimanagement.exception

/**
 * Missing endpoint operation ID exception
 *
 * Raised when an endpoint's `@Operation.operationId` - its persisted identity - is absent or
 * blank.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
class MissingOperationIdException(message: String) : RuntimeException(message)
