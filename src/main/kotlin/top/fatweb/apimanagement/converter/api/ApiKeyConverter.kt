package top.fatweb.apimanagement.converter.api

import com.baomidou.mybatisplus.core.metadata.IPage
import top.fatweb.apimanagement.entity.api.ApiKey
import top.fatweb.apimanagement.param.system.apiKey.ApiKeyAddParam
import top.fatweb.apimanagement.param.system.apiKey.ApiKeyUpdateParam
import top.fatweb.apimanagement.param.system.apiKey.ApiKeyUpdateStatusParam
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.api.ApiKeyVo
import top.fatweb.apimanagement.vo.api.ApiKeyWithSecretVo

/**
 * Convert to ApiKeyVo object
 *
 * @return ApiKeyVo object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ApiKey
 * @see ApiKeyVo
 */
fun ApiKey.toVo() = ApiKeyVo(
    id = this.id,
    userId = this.userId,
    accessKey = this.accessKey,
    name = this.name,
    permissions = this.permissions?.split(",")?.map(String::trim)?.filter(String::isNotEmpty),
    enable = this.enable?.let { it == 1 },
    expireTime = this.expireTime,
    ipWhitelist = this.ipWhitelist,
    rateLimit = this.rateLimit,
    quota = this.quota,
    quotaPeriod = this.quotaPeriod,
    lastUsedTime = this.lastUsedTime,
    remark = this.remark,
    createTime = this.createTime,
    updateTime = this.updateTime
)

/**
 * Convert to ApiKeyWithSecretVo object
 *
 * @param secretKey One-time secret key
 * @return ApiKeyWithSecretVo object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ApiKey
 * @see ApiKeyWithSecretVo
 */
fun ApiKey.toVoWithSecret(secretKey: String) = ApiKeyWithSecretVo(
    apiKey = this.toVo(),
    secretKey = secretKey
)

/**
 * Convert to PageVo<ApiKeyVo> object
 *
 * @return PageVo<ApiKeyVo> object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see IPage
 * @see ApiKey
 * @see PageVo
 */
fun IPage<ApiKey>.toVoPage() = PageVo(
    total = this.total,
    pages = this.pages,
    size = this.size,
    current = this.current,
    records = this.records.map(ApiKey::toVo)
)

/**
 * Convert to ApiKey object
 *
 * @return ApiKey object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ApiKeyAddParam
 * @see ApiKey
 */
fun ApiKeyAddParam.toEntity() = ApiKey().apply {
    name = this@toEntity.name
    enable = 1
    expireTime = this@toEntity.expireTime
    ipWhitelist = this@toEntity.ipWhitelist
    rateLimit = this@toEntity.rateLimit
    quota = this@toEntity.quota
    quotaPeriod = this@toEntity.quotaPeriod
    remark = this@toEntity.remark
}

/**
 * Convert to ApiKey object
 *
 * @return ApiKey object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ApiKeyUpdateParam
 * @see ApiKey
 */
fun ApiKeyUpdateParam.toEntity() = ApiKey().apply {
    id = this@toEntity.id
    name = this@toEntity.name
    expireTime = this@toEntity.expireTime
    ipWhitelist = this@toEntity.ipWhitelist
    rateLimit = this@toEntity.rateLimit
    quota = this@toEntity.quota
    quotaPeriod = this@toEntity.quotaPeriod
    remark = this@toEntity.remark
}

/**
 * Convert to ApiKey object
 *
 * @return ApiKey object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ApiKeyUpdateStatusParam
 * @see ApiKey
 */
fun ApiKeyUpdateStatusParam.toEntity() = ApiKey().apply {
    id = this@toEntity.id
    this.enable = if (this@toEntity.enable == true) 1 else 0
}
