package no.nav.veilarbvedtaksstotte.klagebehandling.service

import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak
import no.nav.veilarbvedtaksstotte.klagebehandling.client.*
import no.nav.veilarbvedtaksstotte.klagebehandling.controller.FormkravKlagefristUnntakSvar
import no.nav.veilarbvedtaksstotte.klagebehandling.controller.FormkravSvar
import no.nav.veilarbvedtaksstotte.klagebehandling.controller.HentKlageRequest
import no.nav.veilarbvedtaksstotte.klagebehandling.domene.*
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

    fun startNyKlagebehandling(klagebehandlingKlageInitiellData: KlageInitiellData) {
        klageRepository.upsertKlagebehandling(KlageBehandling(klageInitiellData = klagebehandlingKlageInitiellData))
    }

    fun oppdaterFormkrav(vedtakId: Long, klagebehandlingKlageFormkravData: KlageFormkravData) {
        val formkravKlagefristOppfylt = klagebehandlingKlageFormkravData.formkravKlagefristOpprettholdt == FormkravSvar.JA
                || (klagebehandlingKlageFormkravData.formkravKlagefristUnntak != null && klagebehandlingKlageFormkravData.formkravKlagefristUnntak != FormkravKlagefristUnntakSvar.NEI)

        val alleFormkravOppfylt =
            klagebehandlingKlageFormkravData.formkravSignert == FormkravSvar.JA
                    && klagebehandlingKlageFormkravData.formkravPart == FormkravSvar.JA
                    && klagebehandlingKlageFormkravData.formkravKonkret == FormkravSvar.JA
                    && formkravKlagefristOppfylt

        val formkravOppfyltString =
            if (alleFormkravOppfylt) FormkravOppfylt.OPPFYLT else FormkravOppfylt.IKKE_OPPFYLT

        klageRepository.updateFormkrav(
            vedtakId,
            klagebehandlingKlageFormkravData,
            formkravOppfyltString
        )

        if (!alleFormkravOppfylt) {
            klageRepository.updateResultat(
                vedtakId,
                Resultat.AVVIST,
                klagebehandlingKlageFormkravData.formkravBegrunnelseIntern
            )
        }
    }

    fun hentKlage(vedtakId: Long): KlageBehandling? {
        return klageRepository.hentKlageBehandling(vedtakId)
    }

    fun sendKlageTilKabal(hentKlageRequest: HentKlageRequest) {
        val lagretKlage = klageRepository.hentKlageBehandling(hentKlageRequest.vedtakId)
            ?: throw KlageIkkeFunnetException(hentKlageRequest.vedtakId)
        val lagretVedtak = vedtakRepository.hentVedtak(hentKlageRequest.vedtakId)
        val kabalDto = mapTilKabalDTO(lagretKlage, lagretVedtak)

        try {
            kabalClient.sendKlageTilKabal(kabalDto)
            klageRepository.updateStatus(hentKlageRequest.vedtakId, Status.SENDT_TIL_KABAL)
            logger.info("Klage sendt til Kabal og status oppdatert for vedtakId ${hentKlageRequest.vedtakId}")
        } catch (e: Exception) {
            logger.error("Feil ved sending av klage til Kabal for vedtakId ${hentKlageRequest.vedtakId}", e)
            throw e
        }

    }

    private fun mapTilKabalDTO(lagretKlage: KlageBehandling, lagretVedtak: Vedtak): KabalDTO {
        return KabalDTO(
            sakenGjelder = Part(
                id = PartId(
                    verdi = lagretKlage.klageInitiellData.norskIdent
                )
            ),
            fagsak = Fagsak(
                fagsakId = "134132412", //mockverdi - må avklares
                fagsystem = "ARBEIDSOPPFOLGING" //mockverdi - må avklares
            ),
            kildeReferanse = lagretKlage.klageInitiellData.vedtakId.toString(),
            forrigeBehandlendeEnhet = lagretVedtak.oppfolgingsenhetId.toString(),
            tilknyttedeJournalposter = listOf(
                TilknyttetJournalpost(
                    type = "OPPRINNELIG_VEDTAK",
                    journalpostId = lagretVedtak.journalpostId
                ),
                TilknyttetJournalpost(
                    type = "BRUKERS_KLAGE",
                    journalpostId = lagretKlage.klageInitiellData.klageJournalpostid
                )
            ),
            brukersKlageMottattVedtaksinstans = lagretKlage.klageInitiellData.klageDato.toString(),
            ytelse = "OMS_OMP", //mockverdi - må avklares
            hjemler = listOf("FTRL_9_2"), //mockverdi - må avklares
            kommentar = "Kommentar fra veileder", // mockverdi - vurder å lage inputfelt ved resultat MEDHOLD
        )
    }

}

class KlageIkkeFunnetException(vedtakId: Long) :
    RuntimeException("Fant ingen klage for vedtakId $vedtakId")
