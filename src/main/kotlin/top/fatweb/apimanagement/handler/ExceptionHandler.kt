package top.fatweb.apimanagement.handler

import com.auth0.jwt.exceptions.JWTDecodeException
import com.auth0.jwt.exceptions.SignatureVerificationException
import com.auth0.jwt.exceptions.TokenExpiredException
import io.swagger.v3.oas.annotations.Hidden
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.jdbc.BadSqlGrammarException
import org.springframework.jdbc.UncategorizedSQLException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.*
import org.springframework.security.web.csrf.InvalidCsrfTokenException
import org.springframework.security.web.csrf.MissingCsrfTokenException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.resource.NoResourceFoundException
import top.fatweb.avatargenerator.AvatarException
import top.fatweb.apimanagement.entity.common.ResponseCode
import top.fatweb.apimanagement.entity.common.ResponseResult
import top.fatweb.apimanagement.exception.*

/**
 * Exception handler
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Hidden
@RestControllerAdvice
class ExceptionHandler {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    /**
     * Handle all exception
     *
     * @param e Exception
     * @return Response object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see Exception
     * @see ResponseResult
     */
    @ExceptionHandler(value = [Exception::class])
    fun exceptionHandler(e: Exception): ResponseResult<*> {
        return when (e) {
            /* Request */
            is HttpRequestMethodNotSupportedException, is NoResourceFoundException -> {
                logger.debug(e.localizedMessage, e)
                ResponseResult.fail(ResponseCode.SYSTEM_REQUEST_ILLEGAL, e.localizedMessage, null)
            }

            is HttpMessageNotReadableException -> {
                logger.debug(e.localizedMessage, e)
                ResponseResult.fail(ResponseCode.SYSTEM_REQUEST_ILLEGAL, e.localizedMessage.split(":")[0], null)
            }

            is MethodArgumentNotValidException -> {
                logger.debug(e.localizedMessage, e)
                val errorMessage = e.allErrors.joinToString(". ") { error -> error.defaultMessage.toString() }
                ResponseResult.fail(ResponseCode.SYSTEM_ARGUMENT_NOT_VALID, errorMessage, null)
            }

            is RequestTooFrequentException -> {
                logger.debug(e.localizedMessage, e)
                ResponseResult.fail(ResponseCode.SYSTEM_REQUEST_TOO_FREQUENT, e.localizedMessage, null)
            }

            /* Authentication */
            is InsufficientAuthenticationException -> {
                logger.debug(e.localizedMessage, e)
                ResponseResult.fail(ResponseCode.PERMISSION_UNAUTHORIZED, e.localizedMessage, null)
            }

            is LockedException -> {
                logger.debug(e.localizedMessage, e)
                ResponseResult.fail(ResponseCode.PERMISSION_USER_LOCKED, "User account has been locked", null)
            }

            is AccountExpiredException -> {
                logger.debug(e.localizedMessage, e)
                ResponseResult.fail(ResponseCode.PERMISSION_USER_EXPIRED, "User account has expired", null)
            }

            is CredentialsExpiredException -> {
                logger.debug(e.localizedMessage, e)
                ResponseResult.fail(
                    ResponseCode.PERMISSION_USER_CREDENTIALS_EXPIRED,
                    "User credentials have expired",
                    null
                )
            }

            is DisabledException -> {
                logger.debug(e.localizedMessage, e)
                ResponseResult.fail(ResponseCode.PERMISSION_USER_DISABLE, "User has been disabled", null)
            }

            is TokenExpiredException, is TokenHasExpiredException -> {
                logger.debug(e.localizedMessage, e)
                ResponseResult.fail(ResponseCode.PERMISSION_TOKEN_HAS_EXPIRED, e.localizedMessage, null)
            }

            is InternalAuthenticationServiceException -> {
                logger.debug(e.localizedMessage, e)
                ResponseResult.fail(ResponseCode.PERMISSION_USERNAME_NOT_FOUND, "Username not found", null)
            }

            is BadCredentialsException -> {
                logger.debug(e.localizedMessage, e)
                ResponseResult.fail(
                    ResponseCode.PERMISSION_LOGIN_USERNAME_PASSWORD_ERROR,
                    "Wrong user name or password",
                    null
                )
            }

            is SignatureVerificationException, is JWTDecodeException -> {
                logger.debug(e.localizedMessage, e)
                ResponseResult.fail(ResponseCode.PERMISSION_TOKEN_ILLEGAL, "Token illegal", null)
            }

            is InvalidCsrfTokenException -> {
                logger.debug(e.localizedMessage, e)
                ResponseResult.fail(ResponseCode.PERMISSION_INVALID_CSRF_TOKEN, e.localizedMessage, null)
            }

            is MissingCsrfTokenException -> {
                logger.debug(e.localizedMessage, e)
                ResponseResult.fail(ResponseCode.PERMISSION_MISSING_CSRF_TOKEN, e.localizedMessage, null)
            }

            is AccessDeniedException -> {
                logger.debug(e.localizedMessage, e)
                ResponseResult.fail(ResponseCode.PERMISSION_ACCESS_DENIED, "Access Denied", null)
            }

            is UserNotFoundException -> {
                logger.debug(e.localizedMessage, e)
                ResponseResult.fail(ResponseCode.PERMISSION_USER_NOT_FOUND, e.localizedMessage, null)
            }

            is LoginFailedException -> {
                logger.debug(e.localizedMessage, e)
                ResponseResult.fail(ResponseCode.PERMISSION_LOGIN_FAILED, e.localizedMessage, null)
            }

            is TokenRefreshErrorException -> {
                logger.debug(e.localizedMessage, e)
                ResponseResult.fail(ResponseCode.PERMISSION_TOKEN_REFRESH_ERROR, e.localizedMessage, null)
            }

            is NoVerificationRequiredException -> {
                logger.debug(e.localizedMessage, e)
                ResponseResult.fail(ResponseCode.PERMISSION_NO_VERIFICATION_REQUIRED, e.localizedMessage, null)
            }

            is VerificationCodeErrorOrExpiredException -> {
                logger.debug(e.localizedMessage, e)
                ResponseResult.fail(ResponseCode.PERMISSION_VERIFY_CODE_ERROR_OR_EXPIRED, e.localizedMessage, null)
            }

            is AccountNeedInitException -> {
                logger.debug(e.localizedMessage, e)
                ResponseResult.fail(ResponseCode.PERMISSION_ACCOUNT_NEED_INIT, e.localizedMessage, null)
            }

            is RetrieveCodeErrorOrExpiredException -> {
                logger.debug(e.localizedMessage, e)
                ResponseResult.fail(ResponseCode.PERMISSION_RETRIEVE_CODE_ERROR_OR_EXPIRED, e.localizedMessage, null)
            }

            is AccountNeedResetPasswordException -> {
                logger.debug(e.localizedMessage, e)
                ResponseResult.fail(ResponseCode.PERMISSION_ACCOUNT_NEED_RESET_PASSWORD, e.localizedMessage, null)
            }

            is InvalidCaptchaCodeException -> {
                logger.debug(e.localizedMessage, e)
                ResponseResult.fail(ResponseCode.SYSTEM_INVALID_CAPTCHA_CODE, e.localizedMessage, null)
            }

            is NeedTwoFactorException -> {
                logger.debug(e.localizedMessage, e)
                ResponseResult.fail(ResponseCode.PERMISSION_NEED_TWO_FACTOR, e.localizedMessage, null)
            }

            is AlreadyHasTwoFactorException -> {
                logger.debug(e.localizedMessage, e)
                ResponseResult.fail(ResponseCode.PERMISSION_ALREADY_HAS_TWO_FACTOR, e.localizedMessage, null)
            }

            is NoTwoFactorFoundException -> {
                logger.debug(e.localizedMessage, e)
                ResponseResult.fail(ResponseCode.PERMISSION_NO_TWO_FACTOR_FOUND, e.localizedMessage, null)
            }

            is TwoFactorVerificationCodeErrorException -> {
                logger.debug(e.localizedMessage, e)
                ResponseResult.fail(
                    ResponseCode.PERMISSION_TWO_FACTOR_VERIFICATION_CODE_ERROR,
                    e.localizedMessage,
                    null
                )
            }

            /* SQL */

            is DatabaseInsertException -> {
                logger.debug(e.localizedMessage, e)
                ResponseResult.databaseFail(ResponseCode.DATABASE_INSERT_FAILED, e.localizedMessage, null)
            }

            is DatabaseUpdateException -> {
                logger.debug(e.localizedMessage, e)
                ResponseResult.databaseFail(ResponseCode.DATABASE_UPDATE_FAILED, e.localizedMessage, null)
            }

            is BadSqlGrammarException -> {
                logger.debug(e.localizedMessage, e)
                ResponseResult.fail(ResponseCode.DATABASE_EXECUTE_ERROR, "Incorrect SQL syntax", null)
            }

            is DuplicateKeyException -> {
                logger.debug(e.localizedMessage, e)
                ResponseResult.fail(ResponseCode.DATABASE_DUPLICATE_KEY, "Duplicate key", null)
            }

            is NoRecordFoundException -> {
                logger.debug(e.localizedMessage, e)
                ResponseResult.fail(ResponseCode.DATABASE_NO_RECORD_FOUND, e.localizedMessage, null)
            }

            is UncategorizedSQLException -> {
                if (e.localizedMessage.contains("SQLITE_CONSTRAINT_UNIQUE")) {
                    logger.debug(e.localizedMessage, e)
                    return ResponseResult.fail(ResponseCode.DATABASE_DUPLICATE_KEY, "Duplicate key", null)
                }

                logger.error(e.localizedMessage, e)
                ResponseResult.fail(ResponseCode.DATABASE_EXECUTE_ERROR, e.localizedMessage, null)
            }

            is RecordAlreadyExistsException -> {
                logger.debug(e.localizedMessage, e)
                ResponseResult.fail(ResponseCode.DATABASE_RECORD_ALREADY_EXISTS, e.localizedMessage, null)
            }

            /* Other */
            is MatchSensitiveWordException -> {
                logger.debug(e.localizedMessage, e)
                ResponseResult.fail(ResponseCode.SYSTEM_MATCH_SENSITIVE_WORD, e.localizedMessage, null)
            }

            is NoEmailConfigException -> {
                logger.debug(e.localizedMessage, e)
                ResponseResult.fail(ResponseCode.SYSTEM_NO_EMAIL_CONFIG, e.localizedMessage, null)
            }

            /* API */
            is AvatarException -> {
                logger.debug(e.localizedMessage, e)
                ResponseResult.fail(ResponseCode.API_AVATAR_ERROR, e.localizedMessage, null)
            }

            /* API Platform */
            is ApiKeyInvalidException -> {
                logger.debug(e.localizedMessage, e)
                ResponseResult.fail(ResponseCode.API_KEY_INVALID, e.localizedMessage, null)
            }

            is ApiKeySecretMismatchException -> {
                logger.debug(e.localizedMessage, e)
                ResponseResult.fail(ResponseCode.API_KEY_SECRET_MISMATCH, e.localizedMessage, null)
            }

            is ApiKeyDisabledException -> {
                logger.debug(e.localizedMessage, e)
                ResponseResult.fail(ResponseCode.API_KEY_DISABLED, e.localizedMessage, null)
            }

            is ApiKeyExpiredException -> {
                logger.debug(e.localizedMessage, e)
                ResponseResult.fail(ResponseCode.API_KEY_EXPIRED, e.localizedMessage, null)
            }

            is ApiKeyIpNotAllowedException -> {
                logger.debug(e.localizedMessage, e)
                ResponseResult.fail(ResponseCode.API_KEY_IP_NOT_ALLOWED, e.localizedMessage, null)
            }

            is ApiKeyPermissionDeniedException -> {
                logger.debug(e.localizedMessage, e)
                ResponseResult.fail(ResponseCode.API_KEY_PERMISSION_DENIED, e.localizedMessage, null)
            }

            is ApiKeyRequiredException -> {
                logger.debug(e.localizedMessage, e)
                ResponseResult.fail(ResponseCode.API_KEY_REQUIRED, e.localizedMessage, null)
            }

            is ApiDisabledException -> {
                logger.debug(e.localizedMessage, e)
                ResponseResult.fail(ResponseCode.API_DISABLED, e.localizedMessage, null)
            }

            is QuotaExceededException -> {
                logger.debug(e.localizedMessage, e)
                ResponseResult.fail(ResponseCode.API_QUOTA_EXCEEDED, e.localizedMessage, null)
            }

            is InsufficientBalanceException -> {
                logger.debug(e.localizedMessage, e)
                ResponseResult.fail(ResponseCode.API_INSUFFICIENT_BALANCE, e.localizedMessage, null)
            }

            is ApiAccountNotFoundException -> {
                logger.debug(e.localizedMessage, e)
                ResponseResult.fail(ResponseCode.API_ACCOUNT_NOT_FOUND, e.localizedMessage, null)
            }

            is ApiUsageIdNotGeneratedException -> {
                logger.debug(e.localizedMessage, e)
                ResponseResult.databaseFail(ResponseCode.API_USAGE_ID_NOT_GENERATED, e.localizedMessage, null)
            }

            else -> {
                logger.error(e.localizedMessage, e)
                ResponseResult.fail(ResponseCode.SYSTEM_ERROR, e.toString(), null)
            }
        }
    }
}
