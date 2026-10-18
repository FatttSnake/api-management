package top.fatweb.apimanagement.config

import jakarta.annotation.PostConstruct
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.springframework.stereotype.Component
import java.security.Security

/**
 * Gateway-owned BouncyCastle provider
 *
 * Registered once at startup so plugins that use the gateway's shared BC always resolve
 * the same provider class, and so a plugin's own BC cannot take over the `"BC"` name.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Component
class BouncyCastleConfig {
    @PostConstruct
    fun init() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }
}
