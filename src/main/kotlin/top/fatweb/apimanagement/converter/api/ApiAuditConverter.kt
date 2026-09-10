package top.fatweb.apimanagement.converter.api

import com.baomidou.mybatisplus.core.metadata.IPage
import top.fatweb.apimanagement.entity.system.EventLog
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.api.ApiAuditVo
import top.fatweb.apimanagement.vo.api.ApiKeyVo
import top.fatweb.apimanagement.vo.permission.UserWithInfoVo

/**
 * Convert to ApiAuditVo object
 *
 * @param resolveUser Resolver from user ID to user information
 * @param resolveKey Resolver from API key ID to key information
 * @param resolveTargetUser Resolver from target user ID to user information
 * @return ApiAuditVo object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see EventLog
 * @see ApiAuditVo
 */
fun EventLog.toAuditVo(
    resolveUser: ((Long) -> UserWithInfoVo?)? = null,
    resolveKey: ((Long) -> ApiKeyVo?)? = null,
    resolveTargetUser: ((Long) -> UserWithInfoVo?)? = null
) = ApiAuditVo(
    id = this.id,
    event = this.event,
    operateUserId = this.operateUserId,
    operateTime = this.operateTime,
    detail = this.detail,
    userVo = this.operateUserId?.let { userId -> resolveUser?.invoke(userId) },
    keyVo = this.apiKeyId?.let { keyId -> resolveKey?.invoke(keyId) },
    targetUserVo = this.targetUserId?.let { targetId -> resolveTargetUser?.invoke(targetId) }
)

/**
 * Convert to PageVo<ApiAuditVo> object
 *
 * @param resolveUser Resolver from user ID to user information
 * @param resolveKey Resolver from API key ID to key information
 * @param resolveTargetUser Resolver from target user ID to user information
 * @return PageVo<ApiAuditVo> object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see IPage
 * @see EventLog
 * @see PageVo
 */
fun IPage<EventLog>.toAuditPage(
    resolveUser: ((Long) -> UserWithInfoVo?)? = null,
    resolveKey: ((Long) -> ApiKeyVo?)? = null,
    resolveTargetUser: ((Long) -> UserWithInfoVo?)? = null
) = PageVo(
    total = this.total,
    pages = this.pages,
    size = this.size,
    current = this.current,
    records = this.records.map { log ->
        log.toAuditVo(
            resolveUser = resolveUser,
            resolveKey = resolveKey,
            resolveTargetUser = resolveTargetUser
        )
    }
)
