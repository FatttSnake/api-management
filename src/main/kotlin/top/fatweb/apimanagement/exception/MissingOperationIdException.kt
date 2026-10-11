package top.fatweb.apimanagement.exception

/**
 * Missing endpoint operation ID exception
 *
 * Raised when an endpoint's `@Operation.operationId` - its persisted identity - is absent or
 * blank. Deliberately not a [PluginInstallException]: a mount refuses such an endpoint before it
 * is ever registered, so reaching this means the endpoint was mounted under a code the request
 * path can no longer derive, and reporting it as an install failure would name the wrong cause.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see RuntimeException
 */
class MissingOperationIdException(message: String) : RuntimeException(message)
