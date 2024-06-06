package no.nav.veilarbvedtaksstotte.client.arbeidssoekeregisteret

import no.nav.common.types.identer.Fnr
import org.springframework.stereotype.Service


@Service
class ArbeidssoekerRegisteretService(
    private val oppslagArbeidssoekerregisteretClientImpl: OppslagArbeidssoekerregisteretClient,

    )  {

    fun hentSisteOpplysningerOmArbeidssoekerMedProfilering(fnr: Fnr): OpplysningerOmArbeidssoekerMedProfilering?{
        return oppslagArbeidssoekerregisteretClientImpl.hentSisteOpplysningerOmArbeidssoekerMedProfilering(fnr)
    }
}