package no.nav.veilarbvedtaksstotte.klagebehandling.service

import no.nav.veilarbvedtaksstotte.klagebehandling.domene.FormkravOppfylt
import no.nav.veilarbvedtaksstotte.klagebehandling.domene.Resultat
import no.nav.veilarbvedtaksstotte.klagebehandling.domene.dto.FormkravRequest
import no.nav.veilarbvedtaksstotte.klagebehandling.domene.dto.InnsendtKlageFraBrukerRequest
import no.nav.veilarbvedtaksstotte.klagebehandling.domene.dto.OpprettKlageRequest
import no.nav.veilarbvedtaksstotte.klagebehandling.repository.KlageRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class KlageService(
    @param:Autowired private val klageRepository: KlageRepository,
) {

    val logger: Logger = LoggerFactory.getLogger(KlageService::class.java)


    fun opprettKlageBehandling(opprettKlageRequest: OpprettKlageRequest) {
        logger.info("Oppretter klagebehandling for vedtakId ${opprettKlageRequest.vedtakId} ")
        klageRepository.upsertOpprettKlagebehandling(opprettKlageRequest)
    }

    fun oppdaterInnsendtKlageFraBruker(innsendtKlageFraBrukerRequest: InnsendtKlageFraBrukerRequest) {
        logger.info("Oppdaterer klagebehandling for vedtakId ${innsendtKlageFraBrukerRequest.vedtakId} med data fra innsendt klage fra bruker")
        klageRepository.upsertKlageBrukerdata(
            innsendtKlageFraBrukerRequest.vedtakId,
            innsendtKlageFraBrukerRequest.klagedato,
            innsendtKlageFraBrukerRequest.klageJournalpostid
        )
    }

    fun oppdaterFormkrav(formkravRequest: FormkravRequest) {
        logger.info("Oppdaterer formkrav for vedtakId ${formkravRequest.vedtakId}")
        val formkravOppfyltString =
            if (formkravRequest.formkravOppfylt) FormkravOppfylt.OPPFYLT else FormkravOppfylt.IKKE_OPPFYLT

        klageRepository.upsertFormkrav(
            formkravRequest.vedtakId,
            formkravOppfyltString,
            formkravRequest.formkravBegrunnelse
        )

        if (!formkravRequest.formkravOppfylt) {
            klageRepository.upsertResultat(
                formkravRequest.vedtakId,
                Resultat.AVVIST,
                formkravRequest.formkravBegrunnelse
            )
        }
    }

}
