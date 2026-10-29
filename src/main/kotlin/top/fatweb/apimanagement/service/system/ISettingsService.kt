package top.fatweb.apimanagement.service.system

import top.fatweb.apimanagement.param.system.ApiSettingsParam
import top.fatweb.apimanagement.param.system.BaseSettingsParam
import top.fatweb.apimanagement.param.system.MailSendParam
import top.fatweb.apimanagement.param.system.MailSettingsParam
import top.fatweb.apimanagement.param.system.TwoFactorSettingsParam
import top.fatweb.apimanagement.vo.api.ApiSettingsVo
import top.fatweb.apimanagement.vo.system.BaseSettingsVo
import top.fatweb.apimanagement.vo.system.MailSettingsVo
import top.fatweb.apimanagement.vo.system.TwoFactorSettingsVo

/**
 * Settings service interface
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
interface ISettingsService {
    /**
     * Get base settings
     *
     * @return BaseSettingsVo object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun getBase(): BaseSettingsVo

    /**
     * Update base settings
     *
     * @param baseSettingsParam Base settings parameters
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun updateBase(baseSettingsParam: BaseSettingsParam)

    /**
     * Get mail settings
     *
     * @return MailSettingsVo object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun getMail(): MailSettingsVo

    /**
     * Update mail settings
     *
     * @param mailSettingsParam Mail settings parameters
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun updateMail(mailSettingsParam: MailSettingsParam)

    /**
     * Send mail
     *
     * @param mailSendParam Send mail parameters
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun sendMail(mailSendParam: MailSendParam)

    /**
     * Get two-factor settings
     *
     * @return TwoFactorSettingsVo object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun getTwoFactor(): TwoFactorSettingsVo

    /**
     * Update two-factor settings
     *
     * @param twoFactorSettingsParam Two-factor settings parameters
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun updateTwoFactor(twoFactorSettingsParam: TwoFactorSettingsParam)

    /**
     * Get API platform settings
     *
     * @return ApiSettingsVo object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun getApi(): ApiSettingsVo

    /**
     * Update API platform settings
     *
     * @param apiSettingsParam API platform settings parameters
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun updateApi(apiSettingsParam: ApiSettingsParam)
}
