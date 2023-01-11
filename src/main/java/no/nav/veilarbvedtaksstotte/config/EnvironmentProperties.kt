package no.nav.veilarbvedtaksstotte.config

import org.springframework.boot.context.properties.ConfigurationProperties


@ConfigurationProperties(prefix = "app.env")
data class EnvironmentProperties (
    val openAmDiscoveryUrl: String,
    val veilarbloginOpenAmClientId: String,
    val stsDiscoveryUrl: String,
    val openAmRefreshUrl: String,
    val abacUrl: String,
    val dbUrl: String,
    val unleashUrl: String,
    val norg2Url: String,
    val naisAadDiscoveryUrl: String,
    val naisAadClientId: String,
    val naisAadIssuer: String,
    val poaoTilgangUrl: String,
    val poaoTilgangScope: String
)