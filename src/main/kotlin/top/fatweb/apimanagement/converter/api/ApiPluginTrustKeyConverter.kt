package top.fatweb.apimanagement.converter.api

import com.baomidou.mybatisplus.core.metadata.IPage
import top.fatweb.apimanagement.entity.api.ApiPluginTrustKey
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.api.ApiPluginTrustKeyVo

/**
 * Convert to ApiPluginTrustKeyVo object
 *
 * @return ApiPluginTrustKeyVo object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ApiPluginTrustKey
 * @see ApiPluginTrustKeyVo
 */
fun ApiPluginTrustKey.toVo() = ApiPluginTrustKeyVo(
    id = this.id,
    keyId = this.keyId,
    alias = this.alias,
    enable = this.enable?.let { it == 1 },
    createTime = this.createTime
)

/**
 * Convert to PageVo<ApiPluginTrustKey> object
 *
 * @return PageVo<ApiPluginTrustKey> object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see IPage
 * @see ApiPluginTrustKey
 * @see PageVo
 */
fun IPage<ApiPluginTrustKey>.toVoPage() = PageVo(
    total = this.total,
    pages = this.pages,
    size = this.size,
    current = this.current,
    records = this.records.map(ApiPluginTrustKey::toVo)
)
