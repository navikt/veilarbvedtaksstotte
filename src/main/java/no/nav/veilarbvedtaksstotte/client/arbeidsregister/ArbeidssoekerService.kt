package no.nav.veilarbvedtaksstotte.client.arbeidsregister

import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.utils.SecureLog.secureLog
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*


@Service
class ArbeidssoekerService(
    private val oppslagArbeidssoekerregisteretClient: OppslagArbeidssoekerregisteretClient,
    private val aktorOppslagClient: AktorOppslagClient

) {
    /**
     * Henter og lagrer arbeidssøkerdata for bruker med aktørId.
     * Med arbeidssøkerdata menes:
     *
     * - Siste arbeidssøkerperiode
     * - Siste opplysninger om arbeidssøker
     * - Siste profilering av bruker
     */
    @Transactional
    fun hentOgLagreArbeidssoekerdataForBruker(aktorId: AktorId) {
        val fnr: Fnr? = aktorOppslagClient.hentFnr(aktorId)
        if (fnr == null) {
            secureLog.info("Fant ingen fødselsnummer for bruker med aktorId: $aktorId")
            throw RuntimeException("Fant ingen fødselsnummer for bruker")
        }

        secureLog.info("Henter arbeidssøkerperioder for bruker med fnr: $fnr")
        val aktivArbeidssoekerperiode: ArbeidssokerperiodeResponse? =
            oppslagArbeidssoekerregisteretClient.hentArbeidssokerPerioder(fnr.get())
                ?.find { it.avsluttet == null }

        if (aktivArbeidssoekerperiode == null) {
            secureLog.info("Fant ingen aktiv arbeidssøkerperiode for bruker med fnr: $fnr")
            return
        }


        val sisteOpplysningerOmArbeidssoeker =
            hentSisteOpplysningerOmArbeidssoeker(fnr, aktivArbeidssoekerperiode.periodeId)

        secureLog.info("Henter opplysninger om arbeidssøker for bruker med fnr: $fnr")
        if (sisteOpplysningerOmArbeidssoeker == null) {
            secureLog.info("Fant ingen opplysninger om arbeidssøker for bruker med fnr: $fnr")
            return
        }

        //sisteOpplysningerOmArbeidssoeker.toOpplysningerOmArbeidssoeker()
        secureLog.info("Lagret opplysninger om arbeidssøker for bruker med fnr: $fnr")

        val sisteProfilering: ProfileringResponse? =
            hentSisteProfilering(
                fnr,
                aktivArbeidssoekerperiode.periodeId,
                sisteOpplysningerOmArbeidssoeker.opplysningerOmArbeidssoekerId
            )

        if (sisteProfilering == null) {
            secureLog.info("Fant ingen profilering for bruker med fnr: $fnr")
            return
        }

        //sisteProfilering.toProfilering()
        secureLog.info("Lagret profilering for bruker med fnr: $fnr")
    }


    private fun hentSisteProfilering(
        fnr: Fnr,
        arbeidssoekerPeriodeId: UUID,
        opplysningerOmArbeidssoekerId: UUID
    ): ProfileringResponse? {
        secureLog.info("Henter profilering for bruker med fnr: $fnr")
        val sisteProfilering: ProfileringResponse? =
            oppslagArbeidssoekerregisteretClient.hentProfilering(fnr.get(), arbeidssoekerPeriodeId)
                ?.filter { it.opplysningerOmArbeidssoekerId == opplysningerOmArbeidssoekerId }
                ?.maxByOrNull { it.sendtInnAv.tidspunkt }
        return sisteProfilering
    }

    private fun hentSisteOpplysningerOmArbeidssoeker(
        fnr: Fnr,
        periodeId: UUID
    ): OpplysningerOmArbeidssoekerResponse? {
        val opplysningerOmArbeidssoeker: List<OpplysningerOmArbeidssoekerResponse>? =
            oppslagArbeidssoekerregisteretClient.hentOpplysningerOmArbeidssoeker(
                fnr.get(),
                periodeId
            )
        val sisteOpplysningerOmArbeidssoeker = opplysningerOmArbeidssoeker?.maxByOrNull {
            it.sendtInnAv.tidspunkt
        }
        return sisteOpplysningerOmArbeidssoeker
    }
}