package top.fatweb.apimanagement.util

import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.mail.javamail.MimeMessageHelper
import top.fatweb.apimanagement.exception.NoEmailConfigException
import top.fatweb.apimanagement.settings.MailSecurityType
import top.fatweb.apimanagement.settings.MailSettings
import top.fatweb.apimanagement.settings.SettingsOperator
import java.util.*

/**
 * Mail util
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
object MailUtil {
    private val mailSender: JavaMailSenderImpl = JavaMailSenderImpl()

    init {
        init()
    }

    fun init() {
        mailSender.defaultEncoding = Charsets.UTF_8.name()
        mailSender.protocol = "smtp"
        mailSender.host = SettingsOperator.getValue(MailSettings::host)
        SettingsOperator.getValue(MailSettings::port)?.let { mailSender.port = it }
        mailSender.username = SettingsOperator.getValue(MailSettings::username)
        mailSender.password = SettingsOperator.getValue(MailSettings::password)

        val properties = Properties()
        when (SettingsOperator.getValue(MailSettings::securityType)) {
            MailSecurityType.SSL_TLS -> properties.setProperty("mail.smtp.ssl.enable", "true")
            MailSecurityType.START_TLS -> properties.setProperty("mail.smtp.starttls.enable", "true")
            else -> {}
        }
        properties["mail.smtp.timeout"] = "10000"
        mailSender.javaMailProperties = properties
    }

    fun sendSimpleMail(subject: String, text: String, html: Boolean = false, vararg to: String) {
        val fromName = SettingsOperator.getValue(MailSettings::fromName) ?: throw NoEmailConfigException("fromName")
        val from = SettingsOperator.getValue(MailSettings::from) ?: throw NoEmailConfigException("from")

        val mimeMessage = mailSender.createMimeMessage()
        val messageHelper = MimeMessageHelper(mimeMessage, true)
        messageHelper.apply {
            setSubject(subject)
            setFrom(from, fromName)
            setSentDate(Date())
            setTo(to)
            setText(text, html)
        }
        mailSender.send(mimeMessage)
    }
}
