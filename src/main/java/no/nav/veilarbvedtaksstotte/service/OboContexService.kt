package no.nav.veilarbvedtaksstotte.service

import no.nav.common.auth.context.AuthContextHolder
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient
import no.nav.common.utils.AuthUtils
import no.nav.veilarbvedtaksstotte.utils.DownstreamApi
import org.springframework.stereotype.Service
import java.util.function.Supplier

@Service
class OboContexService(
    val aadOboTokenClient: AzureAdOnBehalfOfTokenClient,
    val authContext: AuthContextHolder
) {

    fun userTokenSupplier(receivingApp: DownstreamApi): Supplier<String> {
        return Supplier {
            AuthUtils.bearerToken(getAadOboTokenForTjeneste(authContext.requireIdTokenString(), receivingApp))
        }
    }

    private fun getAadOboTokenForTjeneste(token: String, api: DownstreamApi): String {
        val serviceNameWithOptionalEnvironment = api.serviceEnvironment?.let { api.serviceName + it } ?: api.serviceName
        val scope = "api://" + api.cluster + "." + api.namespace + "." + serviceNameWithOptionalEnvironment + "/.default"
        return aadOboTokenClient.exchangeOnBehalfOfToken(scope, token)
    }

}