package no.nav.veilarbvedtaksstotte.client.arbeidssoekeregisteret

import lombok.extern.slf4j.Slf4j
import no.nav.common.types.identer.Fnr
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException


@Service
@Slf4j
class ArbeidssoekerRegisteretService(
    private val oppslagArbeidssoekerregisteretClientImpl: OppslagArbeidssoekerregisteretClient,
    )  {

    fun hentSisteOpplysningerOmArbeidssoekerMedProfilering(fnr: Fnr): OpplysningerOmArbeidssoekerMedProfilering?{
        try {
            return oppslagArbeidssoekerregisteretClientImpl.hentSisteOpplysningerOmArbeidssoekerMedProfilering(fnr)
        }
        catch (e: Exception){
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Feil ved kall mot arbeidssøkerregistret "
            )
        }

    }
}