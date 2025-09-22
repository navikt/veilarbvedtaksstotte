package no.nav.veilarbvedtaksstotte.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.env")
data class EnvironmentProperties (
    val aiaBackendScope: String,
    val aiaBackendUrl: String,
    val dbUrl: String,
    val dokarkivScope: String,
    val dokarkivUrl: String,
    val dokdistfordelingScope: String,
    val dokdistfordelingUrl: String,
    val dokdistkanalScope: String,
    val dokdistkanalUrl: String,
    val naisAadClientId: String,
    val naisAadDiscoveryUrl: String,
    val naisAadIssuer: String,
    val naisAppImage: String,
    val norg2Url: String,
    val pdlScope: String,
    val pdlUrl: String,
    val poaoTilgangScope: String,
    val poaoTilgangUrl: String,
    val ptoPdfgenUrl: String,
    val regoppslagScope: String,
    val regoppslagUrl: String,
    val safScope: String,
    val safUrl: String,
    val tokenxClientId: String,
    val tokenxDiscoveryUrl: String,
    val unleashApiToken: String,
    val unleashUrl: String,
    val veilarbarenaScope: String,
    val veilarbarenaUrl: String,
    val veilarboppfolgingScope: String,
    val veilarboppfolgingUrl: String,
    val veilarbpersonScope: String,
    val veilarbpersonUrl: String,
    val veilarbveilederScope: String,
    val veilarbveilederUrl: String
)
