package top.fatweb.apimanagement.controller.system

import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import top.fatweb.apimanagement.annotation.BaseController
import top.fatweb.apimanagement.annotation.ProcessParam
import top.fatweb.apimanagement.entity.common.ResponseCode
import top.fatweb.apimanagement.entity.common.ResponseResult
import top.fatweb.apimanagement.param.system.*
import top.fatweb.apimanagement.service.system.ISensitiveWordService
import top.fatweb.apimanagement.service.system.ISettingsService
import top.fatweb.apimanagement.vo.api.ApiSettingsVo
import top.fatweb.apimanagement.vo.system.BaseSettingsVo
import top.fatweb.apimanagement.vo.system.MailSettingsVo
import top.fatweb.apimanagement.vo.system.SensitiveWordVo
import top.fatweb.apimanagement.vo.system.TwoFactorSettingsVo

/**
 * System settings management controller
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ISettingsService
 * @see ISensitiveWordService
 */
@BaseController(path = ["/system/settings"], name = "系统设置", description = "系统设置相关接口")
class SettingsController(
    private val settingsService: ISettingsService,
    private val sensitiveWordService: ISensitiveWordService
) {
    /**
     * Get base settings
     *
     * @return Response object includes base settings information
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ResponseResult
     * @see BaseSettingsVo
     */
    @Operation(summary = "获取基础设置")
    @GetMapping("/base")
    @PreAuthorize("hasAnyAuthority('system:settings:base:query')")
    fun getBase(): ResponseResult<BaseSettingsVo> =
        ResponseResult.success(data = settingsService.getBase())

    /**
     * Update base settings
     *
     * @param baseSettingsParam Base settings parameters
     * @return Response object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see BaseSettingsParam
     * @see ResponseResult
     */
    @Operation(summary = "更新基础设置")
    @PutMapping("/base")
    @PreAuthorize("hasAnyAuthority('system:settings:base:modify')")
    fun updateBase(@ProcessParam @RequestBody baseSettingsParam: BaseSettingsParam): ResponseResult<Unit> {
        settingsService.updateBase(baseSettingsParam)

        return ResponseResult.success()
    }

    /**
     * Get mail settings
     *
     * @return Response object includes mail settings
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ResponseResult
     * @see MailSettingsVo
     */
    @Operation(summary = "获取邮件设置")
    @GetMapping("/mail")
    @PreAuthorize("hasAnyAuthority('system:settings:mail:query')")
    fun getMail(): ResponseResult<MailSettingsVo> =
        ResponseResult.success(data = settingsService.getMail())

    /**
     * Update mail settings
     *
     * @param mailSettingsParam Mail settings parameters
     * @return Response object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see MailSettingsParam
     * @see ResponseResult
     */
    @Operation(summary = "更新邮件设置")
    @PutMapping("/mail")
    @PreAuthorize("hasAnyAuthority('system:settings:mail:modify')")
    fun updateMail(@ProcessParam @RequestBody mailSettingsParam: MailSettingsParam): ResponseResult<Unit> {
        settingsService.updateMail(mailSettingsParam)

        return ResponseResult.success()
    }

    /**
     * Send mail test
     *
     * @param mailSendParam Mail send parameters
     * @return Response object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see MailSendParam
     * @see ResponseResult
     */
    @Operation(summary = "邮件发送测试")
    @PostMapping("/mail")
    @PreAuthorize("hasAnyAuthority('system:settings:mail:modify')")
    fun sendMail(@ProcessParam @RequestBody @Valid mailSendParam: MailSendParam): ResponseResult<Unit> {
        settingsService.sendMail(mailSendParam)

        return ResponseResult.success()
    }

    /**
     * Get sensitive word settings
     *
     * @return Response object includes sensitive word settings information
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ResponseResult
     * @see SensitiveWordVo
     */
    @Operation(summary = "获取敏感词配置")
    @GetMapping("/sensitive")
    @PreAuthorize("hasAnyAuthority('system:settings:sensitive:query')")
    fun getSensitive(): ResponseResult<List<SensitiveWordVo>> =
        ResponseResult.databaseSuccess(ResponseCode.DATABASE_SELECT_SUCCESS, data = sensitiveWordService.get())

    /**
     * Add sensitive word
     *
     * @param sensitiveWordAddParam Add sensitive word settings parameters
     * @return Response object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see SensitiveWordAddParam
     * @see ResponseResult
     */
    @Operation(summary = "添加敏感词")
    @PostMapping("/sensitive")
    @PreAuthorize("hasAnyAuthority('system:settings:sensitive:modify')")
    fun addSensitive(@ProcessParam @RequestBody @Valid sensitiveWordAddParam: SensitiveWordAddParam): ResponseResult<Unit> {
        sensitiveWordService.add(sensitiveWordAddParam)

        return ResponseResult.databaseSuccess(ResponseCode.DATABASE_INSERT_SUCCESS)
    }

    /**
     * Update sensitive word
     *
     * @param sensitiveWordUpdateParam Update sensitive word settings parameters
     * @return Response object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see SensitiveWordUpdateParam
     * @see ResponseResult
     */
    @Operation(summary = "修改敏感词")
    @PutMapping("/sensitive")
    @PreAuthorize("hasAnyAuthority('system:settings:sensitive:modify')")
    fun updateSensitive(@RequestBody sensitiveWordUpdateParam: SensitiveWordUpdateParam): ResponseResult<Unit> {
        sensitiveWordService.update(sensitiveWordUpdateParam)

        return ResponseResult.databaseSuccess(ResponseCode.DATABASE_UPDATE_SUCCESS)
    }

    /**
     * Delete sensitive word
     *
     * @see id Sensitive word ID
     * @return Response object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ResponseResult
     */
    @Operation(summary = "删除敏感词")
    @DeleteMapping("/sensitive/{id}")
    @PreAuthorize("hasAnyAuthority('system:settings:sensitive:modify')")
    fun deleteSensitive(@PathVariable id: Long): ResponseResult<Unit> {
        sensitiveWordService.delete(id)

        return ResponseResult.databaseSuccess(ResponseCode.DATABASE_DELETE_SUCCESS)
    }

    /**
     * Get two-factor settings
     *
     * @return Response object includes two-factor settings information
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ResponseResult
     * @see TwoFactorSettingsVo
     */
    @Operation(summary = "获取双因素设置")
    @GetMapping("/two-factor")
    @PreAuthorize("hasAnyAuthority('system:settings:two-factor:query')")
    fun getTwoFactor(): ResponseResult<TwoFactorSettingsVo> =
        ResponseResult.success(data = settingsService.getTwoFactor())

    /**
     * Update two-factor settings
     *
     * @param twoFactorSettingsParam Two-factor settings parameters
     * @return Response object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see TwoFactorSettingsParam
     * @see ResponseResult
     */
    @Operation(summary = "更新双因素设置")
    @PutMapping("/two-factor")
    @PreAuthorize("hasAnyAuthority('system:settings:two-factor:modify')")
    fun updateTwoFactor(@ProcessParam @RequestBody twoFactorSettingsParam: TwoFactorSettingsParam): ResponseResult<Unit> {
        settingsService.updateTwoFactor(twoFactorSettingsParam)

        return ResponseResult.success()
    }

    /**
     * Get API platform settings
     *
     * @return Response object includes API platform settings information
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ResponseResult
     * @see top.fatweb.apimanagement.vo.api.ApiSettingsVo
     */
    @Operation(summary = "获取 API 平台设置")
    @GetMapping("/api")
    @PreAuthorize("hasAnyAuthority('system:settings:api:query')")
    fun getApi(): ResponseResult<ApiSettingsVo> =
        ResponseResult.success(data = settingsService.getApi())

    /**
     * Update API platform settings
     *
     * @param apiSettingsParam API platform settings parameters
     * @return Response object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiSettingsParam
     * @see ResponseResult
     */
    @Operation(summary = "更新 API 平台设置")
    @PutMapping("/api")
    @PreAuthorize("hasAnyAuthority('system:settings:api:modify')")
    fun updateApi(@ProcessParam @RequestBody apiSettingsParam: ApiSettingsParam): ResponseResult<Unit> {
        settingsService.updateApi(apiSettingsParam)

        return ResponseResult.success()
    }
}
