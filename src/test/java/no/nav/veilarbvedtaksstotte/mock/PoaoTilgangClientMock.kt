package no.nav.veilarbvedtaksstotte.mock

import no.nav.poao_tilgang.api.dto.response.Diskresjonskode
import no.nav.poao_tilgang.api.dto.response.TilgangsattributterResponse
import no.nav.poao_tilgang.client.AdGruppe
import no.nav.poao_tilgang.client.Decision
import no.nav.poao_tilgang.client.NorskIdent
import no.nav.poao_tilgang.client.PoaoTilgangClient
import no.nav.poao_tilgang.client.PolicyInput
import no.nav.poao_tilgang.client.PolicyRequest
import no.nav.poao_tilgang.client.PolicyResult
import no.nav.poao_tilgang.client.api.ApiResult
import no.nav.veilarbvedtaksstotte.utils.TestData
import java.util.*

class PoaoTilgangClientMock : PoaoTilgangClient {
    override fun erSkjermetPerson(norskeIdenter: List<NorskIdent>): ApiResult<Map<NorskIdent, Boolean>> {
        TODO("Not yet implemented")
    }

    override fun erSkjermetPerson(norskIdent: NorskIdent): ApiResult<Boolean> {
        TODO("Not yet implemented")
    }

    override fun evaluatePolicies(requests: List<PolicyRequest>): ApiResult<List<PolicyResult>> {
        TODO("Not yet implemented")
    }

    override fun evaluatePolicy(input: PolicyInput): ApiResult<Decision> {
        return ApiResult.success(Decision.Permit)
    }

    override fun hentAdGrupper(navAnsattAzureId: UUID): ApiResult<List<AdGruppe>> {
        TODO("Not yet implemented")
    }

    override fun hentTilgangsAttributter(norskIdent: NorskIdent): ApiResult<TilgangsattributterResponse> {
        return ApiResult.success(TilgangsattributterResponse(
            kontor = TestData.TEST_NAVKONTOR,
            skjermet = false,
            diskresjonskode = Diskresjonskode.UGRADERT
        ))
    }

}
