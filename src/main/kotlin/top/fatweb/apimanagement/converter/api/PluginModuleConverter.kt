package top.fatweb.apimanagement.converter.api

import com.baomidou.mybatisplus.core.metadata.IPage
import top.fatweb.apimanagement.entity.api.PluginModule
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.api.PluginModuleVo

/**
 * Convert to PluginModuleVo object.
 *
 * @return PluginModuleVo object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
fun PluginModule.toVo() = PluginModuleVo(
    id = this.id,
    groupId = this.groupId,
    artifactId = this.artifactId,
    version = this.moduleVersion,
    sha256 = this.sha256,
    size = this.size,
    source = this.source,
    installTime = this.installTime,
    createTime = this.createTime,
    updateTime = this.updateTime
)

/**
 * Convert to a module page.
 *
 * @return module page
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
fun IPage<PluginModule>.toVoPage(): PageVo<PluginModuleVo> = PageVo(
    total = total,
    pages = pages,
    size = size,
    current = current,
    records = records.map(PluginModule::toVo)
)
