package no.nav.veilarbvedtaksstotte.service

import com.nimbusds.jwt.JWTClaimsSet
import no.nav.common.auth.context.AuthContextHolder
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient
import no.nav.common.utils.AuthUtils
import no.nav.veilarbvedtaksstotte.config.EnvironmentProperties
import no.nav.veilarbvedtaksstotte.utils.DownstreamApi
import org.springframework.stereotype.Service
import java.util.function.Supplier

@Service
class ContextAwareService(
    val environmentProperties: EnvironmentProperties,
    val aadOboTokenClient: AzureAdOnBehalfOfTokenClient,
    val authContext: AuthContextHolder
) {

    fun contextAwareUserTokenSupplier(receivingApp: DownstreamApi): Supplier<String> {
        val azureAdIssuer = environmentProperties.naisAadIssuer
        return Supplier {
            val token: String = authContext.requireIdTokenString()
            val tokenIssuer: String = authContext.idTokenClaims
                .map { obj: JWTClaimsSet -> obj.issuer }
                .orElseThrow()
            AuthUtils.bearerToken(
                if (azureAdIssuer == tokenIssuer) getAadOboTokenForTjeneste(
                    token,
                    receivingApp
                ) else token
            )
        }
    }

    private fun getAadOboTokenForTjeneste(token: String, api: DownstreamApi): String {
        val scope = "api://" + api.cluster + "." + api.namespace + "." + api.serviceName + "/.default"
        return aadOboTokenClient.exchangeOnBehalfOfToken(scope, token)
    }

}