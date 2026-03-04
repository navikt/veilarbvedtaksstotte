package no.nav.veilarbvedtaksstotte.klagebehandling.service

import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak
import no.nav.veilarbvedtaksstotte.klagebehandling.client.*
import no.nav.veilarbvedtaksstotte.klagebehandling.controller.dto.*
import no.nav.veilarbvedtaksstotte.klagebehandling.domene.FormkravOppfylt
import no.nav.veilarbvedtaksstotte.klagebehandling.domene.KlageBehandling
import no.nav.veilarbvedtaksstotte.klagebehandling.domene.Resultat
import no.nav.veilarbvedtaksstotte.klagebehandling.domene.Status
import no.nav.veilarbvedtaksstotte.klagebehandling.repository.KlageRepository
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class KlageService(
    @param:Autowired private val klageRepository: KlageRepository,
    @param:Autowired private val kabalClient: KabalClient,
    @param:Autowired private val vedtakRepository: VedtaksstotteRepository

) {

    val logger: Logger = LoggerFactory.getLogger(KlageService::class.java)

    fun opprettKlageBehandling(opprettKlageRequest: OpprettKlageRequest) {
        logger.info("Oppretter klagebehandling for vedtakId ${opprettKlageRequest.vedtakId} ")
        klageRepository.upsertOpprettKlagebehandling(opprettKlageRequest)
    }

    fun oppdaterFormkrav(formkravRequest: FormkravRequest) {
        logger.info("Oppdaterer formkrav for vedtakId ${formkravRequest.vedtakId}")
        val formkravKlagefristOppfylt = formkravRequest.klagefristOpprettholdt == FormkravSvar.JA
                || (formkravRequest.klagefristUnntak != null && formkravRequest.klagefristUnntak != FormkravKlagefristUnntakSvar.NEI)

        val alleFormkravOppfylt =
            formkravRequest.signert == FormkravSvar.JA
                    && formkravRequest.part == FormkravSvar.JA
                    && formkravRequest.konkret == FormkravSvar.JA
                    && formkravKlagefristOppfylt

        val formkravOppfyltString =
            if (alleFormkravOppfylt) FormkravOppfylt.OPPFYLT else FormkravOppfylt.IKKE_OPPFYLT

        klageRepository.updateFormkrav(
            formkravRequest,
            formkravOppfyltString
        )

        if (!alleFormkravOppfylt) {
            klageRepository.updateResultat(
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
        val lagretVedtak = vedtakRepository.hentVedtak(klageRequest.vedtakId)
        val kabalDto = mapTilKabalDTO(lagretKlage!!, lagretVedtak)

        try {
            kabalClient.sendKlageTilKabal(kabalDto)
            klageRepository.updateStatus(klageRequest.vedtakId, Status.SENDT_TIL_KABAL)
            logger.info("Klage sendt til Kabal og status oppdatert for vedtakId ${klageRequest.vedtakId}")
        } catch (e: Exception) {
            logger.error("Feil ved sending av klage til Kabal for vedtakId ${klageRequest.vedtakId}", e)
            throw e
        }

    }

    private fun mapTilKabalDTO(lagretKlage: KlageBehandling, lagretVedtak: Vedtak): KabalDTO {

        return KabalDTO(
            sakenGjelder = Part(
                id = PartId(
                    verdi = lagretKlage.norskIdent
                )
            ),
            fagsak = Fagsak(
                fagsakId = "134132412", //mockverdi
                fagsystem = "ARBEIDSOPPFOLGING" //mockverdi
            ),
            kildeReferanse = lagretKlage.vedtakId.toString(),
            hjemler = listOf("FVL_11"), //mockverdi - ikke avklart
            forrigeBehandlendeEnhet = lagretVedtak.oppfolgingsenhetId.toString(),
            tilknyttedeJournalposter = listOf(
                TilknyttetJournalpost(
                    type = "OPPRINNELIG_VEDTAK",
                    journalpostId = lagretVedtak.journalpostId
                ),
                TilknyttetJournalpost(
                    type = "BRUKERS_KLAGE",
                    journalpostId = lagretKlage.klageJournalpostid!!
                )
            ),
            brukersKlageMottattVedtaksinstans = lagretKlage.klageDato!!,
            ytelse = "OMS_OMP", //mockverdi
            kommentar = "Kommentar fra veileder", // mockverdi
        )
    }

}
