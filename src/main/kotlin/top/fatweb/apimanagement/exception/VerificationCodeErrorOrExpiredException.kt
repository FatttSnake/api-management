package top.fatweb.apimanagement.exception

/**
 * Verification code error or expired exception
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see RuntimeException
 */
class VerificationCodeErrorOrExpiredException : RuntimeException("Verification code is error or has expired")
