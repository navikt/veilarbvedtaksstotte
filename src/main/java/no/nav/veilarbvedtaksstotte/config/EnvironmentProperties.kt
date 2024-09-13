package no.nav.veilarbvedtaksstotte.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.env")
data class EnvironmentProperties (
    val aiaBackendUrl: String,
    val dbUrl: String,
    val naisAadDiscoveryUrl: String,
    val naisAadClientId: String,
    val naisAadIssuer: String,
    val norg2Url: String,
    val poaoTilgangUrl: String,
    val poaoTilgangScope: String,
    val safScope: String,
    val safUrl: String,
    val stsDiscoveryUrl: String,
    val tokenxClientId: String,
    val tokenxDiscoveryUrl: String,
    val unleashApiToken: String,
    val unleashUrl: String,
    val veilarboppfolgingScope: String,
    val veilarboppfolgingUrl: String,
    val veilarbpersonUrl: String,
    val veilarbveilederScope: String,
    val veilarbveilederUrl: String

)
