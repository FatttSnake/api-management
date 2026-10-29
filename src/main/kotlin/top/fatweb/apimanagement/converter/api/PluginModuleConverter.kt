package top.fatweb.apimanagement.converter.api

import com.baomidou.mybatisplus.core.metadata.IPage
import top.fatweb.apimanagement.entity.api.PluginModule
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.api.PluginModuleReferenceVo
import top.fatweb.apimanagement.vo.api.PluginModuleVo

/**
 * Convert to PluginModuleVo object.
 *
 * @return PluginModuleVo object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
fun PluginModule.toVo(referencedBy: List<PluginModuleReferenceVo> = emptyList()) = PluginModuleVo(
    id = this.id,
    groupId = this.groupId,
    artifactId = this.artifactId,
    moduleVersion = this.moduleVersion,
    sha256 = this.sha256,
    size = this.size,
    source = this.source,
    installTime = this.installTime,
    createTime = this.createTime,
    updateTime = this.updateTime,
    referenced = referencedBy.isNotEmpty(),
    referencedBy = referencedBy,
)

/**
 * Convert to a module page.
 *
 * @return module page
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
fun IPage<PluginModule>.toVoPage(
    references: Map<Long, List<PluginModuleReferenceVo>> = emptyMap(),
): PageVo<PluginModuleVo> = PageVo(
    total = total,
    pages = pages,
    size = size,
    current = current,
    records = records.map { module -> module.toVo(module.id?.let(references::get).orEmpty()) },
)
