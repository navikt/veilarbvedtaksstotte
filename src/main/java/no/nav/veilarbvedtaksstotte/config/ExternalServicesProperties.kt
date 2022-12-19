package no.nav.veilarbvedtaksstotte.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "app.services")
data class ExternalServicesProperties(
    val veilarbarenaUrl: String,
    val veilarbarenaTokenScope: String
)