package no.nav.veilarbvedtaksstotte.config

import com.github.benmanes.caffeine.cache.Caffeine
import io.getunleash.DefaultUnleash
import io.getunleash.util.UnleashConfig
import no.nav.common.auth.context.AuthContextHolder
import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.client.aktoroppslag.CachedAktorOppslagClient
import no.nav.common.client.aktoroppslag.PdlAktorOppslagClient
import no.nav.common.client.pdl.PdlClientImpl
import no.nav.common.job.leader_election.LeaderElectionClient
import no.nav.common.job.leader_election.LeaderElectionHttpClient
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient
import no.nav.common.utils.AuthUtils
import no.nav.common.utils.EnvironmentUtils
import no.nav.poao_tilgang.api.dto.response.TilgangsattributterResponse
import no.nav.poao_tilgang.client.*
import no.nav.veilarbvedtaksstotte.client.aiaBackend.AiaBackendClient
import no.nav.veilarbvedtaksstotte.client.aiaBackend.AiaBackendClientImpl
import no.nav.veilarbvedtaksstotte.client.arbeidssoekeregisteret.OppslagArbeidssoekerregisteretClientImpl
import no.nav.veilarbvedtaksstotte.client.arena.VeilarbarenaClient
import no.nav.veilarbvedtaksstotte.client.arena.VeilarbarenaClientImpl
import no.nav.veilarbvedtaksstotte.client.dokarkiv.DokarkivClient
import no.nav.veilarbvedtaksstotte.client.dokarkiv.DokarkivClientImpl
import no.nav.veilarbvedtaksstotte.client.dokarkiv.SafClient
import no.nav.veilarbvedtaksstotte.client.dokarkiv.SafClientImpl
import no.nav.veilarbvedtaksstotte.client.dokdistfordeling.DokdistribusjonClient
import no.nav.veilarbvedtaksstotte.client.dokdistfordeling.DokdistribusjonClientImpl
import no.nav.veilarbvedtaksstotte.client.norg2.Norg2Client
import no.nav.veilarbvedtaksstotte.client.norg2.Norg2ClientImpl
import no.nav.veilarbvedtaksstotte.client.pdf.PdfClient
import no.nav.veilarbvedtaksstotte.client.pdf.PdfClientImpl
import no.nav.veilarbvedtaksstotte.client.person.BehandlingsNummer
import no.nav.veilarbvedtaksstotte.client.person.VeilarbpersonClient
import no.nav.veilarbvedtaksstotte.client.person.VeilarbpersonClientImpl
import no.nav.veilarbvedtaksstotte.client.regoppslag.RegoppslagClient
import no.nav.veilarbvedtaksstotte.client.regoppslag.RegoppslagClientImpl
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.VeilarboppfolgingClient
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.VeilarboppfolgingClientImpl
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.VeilarbveilederClient
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.VeilarbveilederClientImpl
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
class ClientConfig {

    @Bean
    fun arenaClient(
        properties: EnvironmentProperties, machineTokenClient: AzureAdMachineToMachineTokenClient
    ): VeilarbarenaClient {
        return VeilarbarenaClientImpl(
            properties.veilarbarenaUrl,
        ) { machineTokenClient.createMachineToMachineToken(properties.veilarbarenaScope) }
    }

    @Bean
    fun pdfClient(properties: EnvironmentProperties): PdfClient {
        return PdfClientImpl(properties.ptoPdfgenUrl)
    }

    @Bean
    fun egenvurderingClient(
        properties: EnvironmentProperties,
        aadOboTokenClient: AzureAdOnBehalfOfTokenClient,
        authContextHolder: AuthContextHolder
    ): AiaBackendClient {
        return AiaBackendClientImpl(properties.aiaBackendUrl) {
            AuthUtils.bearerToken(
                aadOboTokenClient.exchangeOnBehalfOfToken(
                    properties.aiaBackendScope, authContextHolder.requireIdTokenString()
                )
            )
        }
    }

    @Bean
    fun oppfolgingClient(
        properties: EnvironmentProperties, machineTokenClient: AzureAdMachineToMachineTokenClient
    ): VeilarboppfolgingClient {
        return VeilarboppfolgingClientImpl(
            properties.veilarboppfolgingUrl,
        ) { machineTokenClient.createMachineToMachineToken(properties.veilarboppfolgingScope) }
    }

    @Bean
    fun personClient(
        properties: EnvironmentProperties,
        aadOboTokenClient: AzureAdOnBehalfOfTokenClient,
        authContextHolder: AuthContextHolder,
        machineTokenClient: AzureAdMachineToMachineTokenClient
    ): VeilarbpersonClient {
        return VeilarbpersonClientImpl(
            properties.veilarbpersonUrl,
            {
                AuthUtils.bearerToken(
                    aadOboTokenClient.exchangeOnBehalfOfToken(
                        properties.veilarbpersonScope,
                        authContextHolder.requireIdTokenString()
                    )
                )
            }
        ) { machineTokenClient.createMachineToMachineToken(properties.veilarbpersonScope) }
    }

    @Bean
    fun safClient(
        properties: EnvironmentProperties, machineTokenClient: AzureAdMachineToMachineTokenClient
    ): SafClient {
        return SafClientImpl(properties.safUrl) { machineTokenClient.createMachineToMachineToken(properties.safScope) }
    }

    @Bean
    fun veilederOgEnhetClient(
        properties: EnvironmentProperties,
        authContextHolder: AuthContextHolder,
        aadOboTokenClient: AzureAdOnBehalfOfTokenClient,
        machineTokenClient: AzureAdMachineToMachineTokenClient
    ): VeilarbveilederClient {
        return VeilarbveilederClientImpl(
            properties.veilarbveilederUrl,
            authContextHolder,
            {
                AuthUtils.bearerToken(
                    aadOboTokenClient.exchangeOnBehalfOfToken(
                        properties.veilarbveilederScope,
                        authContextHolder.requireIdTokenString()
                    )
                )
            }
        ) { machineTokenClient.createMachineToMachineToken(properties.veilarbveilederScope) }
    }

    @Bean
    fun dokarkivClient(
        properties: EnvironmentProperties, machineTokenClient: AzureAdMachineToMachineTokenClient
    ): DokarkivClient {
        return DokarkivClientImpl(
            properties.dokarkivUrl,
        ) { machineTokenClient.createMachineToMachineToken(properties.dokarkivScope) }
    }

    @Bean
    fun regoppslagClient(
        properties: EnvironmentProperties, machineTokenClient: AzureAdMachineToMachineTokenClient
    ): RegoppslagClient {
        return RegoppslagClientImpl(properties.regoppslagUrl) {
            machineTokenClient.createMachineToMachineToken(
                properties.regoppslagScope
            )
        }
    }

    @Bean
    fun oppslagArbeidssoekerregisteretClient(
        properties: EnvironmentProperties, machineTokenClient: AzureAdMachineToMachineTokenClient
    ): OppslagArbeidssoekerregisteretClientImpl {
        return OppslagArbeidssoekerregisteretClientImpl(
            properties.veilarbpersonUrl
        ) { machineTokenClient.createMachineToMachineToken(properties.veilarbpersonScope) }
    }

    @Bean
    fun dokDistribusjonClient(
        properties: EnvironmentProperties, machineTokenClient: AzureAdMachineToMachineTokenClient
    ): DokdistribusjonClient {
        // dokdistfordeling bruker saf token scope
        return DokdistribusjonClientImpl(properties.dokdistfordelingUrl) {
            machineTokenClient.createMachineToMachineToken(
                properties.safScope
            )
        }
    }

    @Bean
    fun aktorOppslagClient(
        properties: EnvironmentProperties, tokenClient: AzureAdMachineToMachineTokenClient
    ): AktorOppslagClient {
        val pdlClient = PdlClientImpl(
            properties.pdlUrl,
            { tokenClient.createMachineToMachineToken(properties.pdlScope) },
            BehandlingsNummer.VEDTAKSTOTTE.value
        )
        return CachedAktorOppslagClient(PdlAktorOppslagClient(pdlClient))
    }

    @Bean
    fun unleashClient(properties: EnvironmentProperties): DefaultUnleash = DefaultUnleash(
        UnleashConfig.builder().appName(ApplicationConfig.APPLICATION_NAME)
            .instanceId(ApplicationConfig.APPLICATION_NAME).unleashAPI(properties.unleashUrl)
            .apiKey(properties.unleashApiToken).environment(if (isProduction) "production" else "development").build()
    )

//    @Bean
//    fun influxMetricsClient(): MetricsClient {
//        return InfluxClient()
//    }

    @Bean
    fun leaderElectionClient(): LeaderElectionClient {
        return LeaderElectionHttpClient()
    }

    @Bean
    fun norg2Client(properties: EnvironmentProperties): Norg2Client {
        return Norg2ClientImpl(properties.norg2Url)
    }

    @Bean
    fun poaoTilgangClient(
        properties: EnvironmentProperties, tokenClient: AzureAdMachineToMachineTokenClient
    ): PoaoTilgangClient {
        return PoaoTilgangCachedClient(
            PoaoTilgangHttpClient(properties.poaoTilgangUrl,
                { tokenClient.createMachineToMachineToken(properties.poaoTilgangScope) }),
            tilgangsAttributterCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(1))
                .build()
        )
    }

    companion object {
        private val isProduction: Boolean
            get() = EnvironmentUtils.isProduction().orElseThrow()
    }
}
