package top.fatweb.apimanagement.controller

import org.springframework.web.bind.annotation.GetMapping
import top.fatweb.apimanagement.annotation.HiddenController
import top.fatweb.apimanagement.service.system.ISettingsService
import top.fatweb.apimanagement.vo.metadata.ConfigVo
import top.fatweb.apimanagement.vo.metadata.HealthVo
import java.time.LocalDateTime

/**
 * Metadata controller
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@HiddenController
class MetadataController(
    private val settingsService: ISettingsService
) {
    @GetMapping("/health")
    fun health() =
        HealthVo(
            status = "UP",
            timestamp = LocalDateTime.now()
        )

    @GetMapping("/config")
    fun config() =
        ConfigVo.fromBaseSettingsVo(settingsService.getBase())
}
