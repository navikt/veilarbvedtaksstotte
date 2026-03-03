package no.nav.veilarbvedtaksstotte.klagebehandling.service

import no.nav.veilarbvedtaksstotte.klagebehandling.domene.FormkravOppfylt
import no.nav.veilarbvedtaksstotte.klagebehandling.domene.KlageBehandling
import no.nav.veilarbvedtaksstotte.klagebehandling.domene.Resultat
import no.nav.veilarbvedtaksstotte.klagebehandling.controller.dto.FormkravRequest
import no.nav.veilarbvedtaksstotte.klagebehandling.controller.dto.KlageRequest
import no.nav.veilarbvedtaksstotte.klagebehandling.controller.dto.OpprettKlageRequest
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


    fun oppdaterFormkrav(formkravRequest: FormkravRequest) {
        logger.info("Oppdaterer formkrav for vedtakId ${formkravRequest.vedtakId}")
        val formkravOppfyltString =
            if (formkravRequest.formkravOppfylt) FormkravOppfylt.OPPFYLT else FormkravOppfylt.IKKE_OPPFYLT

        klageRepository.upsertFormkrav(
            formkravRequest.vedtakId,
            formkravOppfyltString,
            formkravRequest.formkravBegrunnelseIntern
        )

        if (!formkravRequest.formkravOppfylt) {
            klageRepository.upsertResultat(
                formkravRequest.vedtakId,
                Resultat.AVVIST,
                formkravRequest.formkravBegrunnelseIntern
            )
        }
    }

    fun hentKlage(klageRequest: KlageRequest): KlageBehandling? {
        logger.info("Henter klage for vedtakId ${klageRequest.vedtakId}")
        return klageRepository.hentKlageBehandling(klageRequest.vedtakId)
    }

    fun sendKlageTilKabal(klageRequest: KlageRequest) {
        logger.info("Sender klage til kabal for vedtakId ${klageRequest.vedtakId}")
        val lagretKlage = klageRepository.hentKlageBehandling(klageRequest.vedtakId)
        val mappetKlage = klageRequest.copy()
        // lag mapper for å mappe klageRequest og evt opprinnelig vedtak til det formatet kabal forventer

        // send klage til api

        // oppdater status i database

    }

}
