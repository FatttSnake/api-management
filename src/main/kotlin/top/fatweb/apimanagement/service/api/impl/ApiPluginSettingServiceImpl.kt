package top.fatweb.apimanagement.service.api.impl

import com.baomidou.dynamic.datasource.annotation.DS
import com.baomidou.mybatisplus.extension.kotlin.KtQueryWrapper
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import top.fatweb.apimanagement.entity.api.ApiPluginSetting
import top.fatweb.apimanagement.mapper.api.ApiPluginSettingMapper
import top.fatweb.apimanagement.service.api.IApiPluginSettingService
import top.fatweb.apimanagement.util.saveOrThrowException
import top.fatweb.apimanagement.util.updateOrThrowException

/**
 * Plugin setting service implement
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ServiceImpl
 * @see ApiPluginSettingMapper
 * @see ApiPluginSetting
 * @see IApiPluginSettingService
 */
@Service
@DS("master")
class ApiPluginSettingServiceImpl : ServiceImpl<ApiPluginSettingMapper, ApiPluginSetting>(), IApiPluginSettingService {
    override fun get(pluginId: String, key: String): String? =
        getOne(
            KtQueryWrapper(ApiPluginSetting())
                .eq(ApiPluginSetting::pluginId, pluginId)
                .eq(ApiPluginSetting::settingKey, key)
        )?.settingValue

    @Transactional
    override fun set(pluginId: String, key: String, value: String) {
        val existing = getOne(
            KtQueryWrapper(ApiPluginSetting())
                .eq(ApiPluginSetting::pluginId, pluginId)
                .eq(ApiPluginSetting::settingKey, key)
        )
        if (existing == null) {
            saveOrThrowException {
                save(ApiPluginSetting().apply {
                    this.pluginId = pluginId
                    this.settingKey = key
                    this.settingValue = value
                })
            }
        } else {
            existing.settingValue = value
            updateOrThrowException { updateById(existing) }
        }
    }

    @Transactional
    override fun deleteByPlugin(pluginId: String) {
        remove(KtQueryWrapper(ApiPluginSetting()).eq(ApiPluginSetting::pluginId, pluginId))
    }
}
