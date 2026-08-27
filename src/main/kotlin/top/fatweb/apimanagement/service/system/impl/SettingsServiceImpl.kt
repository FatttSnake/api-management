package top.fatweb.apimanagement.service.system.impl

import org.springframework.stereotype.Service
import top.fatweb.apimanagement.param.system.ApiSettingsParam
import top.fatweb.apimanagement.param.system.BaseSettingsParam
import top.fatweb.apimanagement.param.system.MailSendParam
import top.fatweb.apimanagement.param.system.MailSettingsParam
import top.fatweb.apimanagement.param.system.TwoFactorSettingsParam
import top.fatweb.apimanagement.properties.ServerProperties
import top.fatweb.apimanagement.service.system.ISettingsService
import top.fatweb.apimanagement.settings.*
import top.fatweb.apimanagement.util.MailUtil
import top.fatweb.apimanagement.util.md5
import top.fatweb.apimanagement.vo.system.ApiSettingsVo
import top.fatweb.apimanagement.vo.system.BaseSettingsVo
import top.fatweb.apimanagement.vo.system.MailSettingsVo
import top.fatweb.apimanagement.vo.system.TwoFactorSettingsVo

/**
 * Settings service implement
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ServerProperties
 * @see ISettingsService
 */
@Service
class SettingsServiceImpl : ISettingsService {
    override fun getBase() = BaseSettingsVo(
        systemName = SettingsOperator.getValue(BaseSettings::systemName, "ApiManagement"),
        tokenExpiryBufferMs = SettingsOperator.getValue(BaseSettings::tokenExpiryBufferMs, 1800000),
        tokenExpiryCheckIntervalMs = SettingsOperator.getValue(BaseSettings::tokenExpiryCheckIntervalMs, 600000),
        turnstileSiteKey = SettingsOperator.getValue(BaseSettings::turnstileSiteKey),
        turnstileSecretKey = SettingsOperator.getValue(BaseSettings::turnstileSecretKey)?.takeIf { it.isNotEmpty() }
            ?.let(::md5),
        homeUrl = SettingsOperator.getValue(BaseSettings::homeUrl, "http://localhost")
    )

    override fun updateBase(baseSettingsParam: BaseSettingsParam) {
        baseSettingsParam.run {
            SettingsOperator.setValue(BaseSettings::systemName, systemName)
            SettingsOperator.setValue(BaseSettings::tokenExpiryBufferMs, tokenExpiryBufferMs)
            SettingsOperator.setValue(BaseSettings::tokenExpiryCheckIntervalMs, tokenExpiryCheckIntervalMs)
            SettingsOperator.setValue(BaseSettings::turnstileSiteKey, turnstileSiteKey)
            SettingsOperator.setValue(BaseSettings::turnstileSecretKey, turnstileSecretKey)
            SettingsOperator.setValue(BaseSettings::homeUrl, homeUrl)
        }
    }

    override fun getMail() = MailSettingsVo(
        host = SettingsOperator.getValue(MailSettings::host, "smtp.example.com"),
        port = SettingsOperator.getValue(MailSettings::port, 25),
        securityType = SettingsOperator.getValue(MailSettings::securityType, MailSecurityType.NONE),
        username = SettingsOperator.getValue(MailSettings::username),
        password = SettingsOperator.getValue(MailSettings::password)?.takeIf { it.isNotEmpty() }?.let(::md5),
        from = SettingsOperator.getValue(MailSettings::from),
        fromName = SettingsOperator.getValue(MailSettings::fromName)
    )

    override fun updateMail(mailSettingsParam: MailSettingsParam) {
        mailSettingsParam.run {
            SettingsOperator.setValue(MailSettings::host, host)
            SettingsOperator.setValue(MailSettings::port, port)
            SettingsOperator.setValue(MailSettings::securityType, securityType)
            SettingsOperator.setValue(MailSettings::username, username)
            SettingsOperator.setValue(MailSettings::password, password)
            SettingsOperator.setValue(MailSettings::from, from)
            SettingsOperator.setValue(MailSettings::fromName, fromName)
        }

        MailUtil.init()
    }

    override fun sendMail(mailSendParam: MailSendParam) {
        mailSendParam.to?.let {
            MailUtil.sendSimpleMail(
                "${SettingsOperator.getValue(BaseSettings::systemName)} Test Message",
                "This is a test email sent when testing the system email sending service.",
                false,
                it
            )
        }
    }

    override fun getTwoFactor() = TwoFactorSettingsVo(
        issuer = SettingsOperator.getValue(TwoFactorSettings::issuer, "ApiManagement"),
        secretKeyLength = SettingsOperator.getValue(TwoFactorSettings::secretKeyLength, 16)
    )

    override fun updateTwoFactor(twoFactorSettingsParam: TwoFactorSettingsParam) {
        twoFactorSettingsParam.run {
            SettingsOperator.setValue(TwoFactorSettings::issuer, issuer)
            SettingsOperator.setValue(TwoFactorSettings::secretKeyLength, secretKeyLength)
        }
    }

    override fun getApi() = ApiSettingsVo(
        defaultRateLimitPerMin = SettingsOperator.getValue(ApiSettings::defaultRateLimitPerMin, 0),
        defaultQuota = SettingsOperator.getValue(ApiSettings::defaultQuota, 0),
        defaultQuotaPeriodSeconds = SettingsOperator.getValue(ApiSettings::defaultQuotaPeriodSeconds, 86400),
        accessKeyLength = SettingsOperator.getValue(ApiSettings::accessKeyLength, 20),
        secretKeyLength = SettingsOperator.getValue(ApiSettings::secretKeyLength, 40),
        balanceCheckEnabled = SettingsOperator.getValue(ApiSettings::balanceCheckEnabled, true),
        cacheTtlSeconds = SettingsOperator.getValue(ApiSettings::cacheTtlSeconds, 300)
    )

    override fun updateApi(apiSettingsParam: ApiSettingsParam) {
        apiSettingsParam.run {
            SettingsOperator.setValue(ApiSettings::defaultRateLimitPerMin, defaultRateLimitPerMin)
            SettingsOperator.setValue(ApiSettings::defaultQuota, defaultQuota)
            SettingsOperator.setValue(ApiSettings::defaultQuotaPeriodSeconds, defaultQuotaPeriodSeconds)
            SettingsOperator.setValue(ApiSettings::accessKeyLength, accessKeyLength)
            SettingsOperator.setValue(ApiSettings::secretKeyLength, secretKeyLength)
            SettingsOperator.setValue(ApiSettings::balanceCheckEnabled, balanceCheckEnabled)
            SettingsOperator.setValue(ApiSettings::cacheTtlSeconds, cacheTtlSeconds)
        }
    }
}
