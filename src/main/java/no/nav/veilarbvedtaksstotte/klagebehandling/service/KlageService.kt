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
        klageRepository.upsertOpprettKlagebehandling(opprettKlageRequest)
    }

    fun oppdaterFormkrav(formkravRequest: FormkravRequest) {
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
        return klageRepository.hentKlageBehandling(klageRequest.vedtakId)
    }

    fun sendKlageTilKabal(klageRequest: KlageRequest) {
        val lagretKlage = klageRepository.hentKlageBehandling(klageRequest.vedtakId)
            ?: throw KlageIkkeFunnetException(klageRequest.vedtakId)
        val lagretVedtak = vedtakRepository.hentVedtak(klageRequest.vedtakId)
        val kabalDto = mapTilKabalDTO(lagretKlage, lagretVedtak)

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
                fagsakId = "134132412", //mockverdi - må avklares
                fagsystem = "ARBEIDSOPPFOLGING" //mockverdi - må avklares
            ),
            kildeReferanse = lagretKlage.vedtakId.toString(),
            forrigeBehandlendeEnhet = lagretVedtak.oppfolgingsenhetId.toString(),
            tilknyttedeJournalposter = listOf(
                TilknyttetJournalpost(
                    type = "OPPRINNELIG_VEDTAK",
                    journalpostId = lagretVedtak.journalpostId
                ),
                TilknyttetJournalpost(
                    type = "BRUKERS_KLAGE",
                    journalpostId = lagretKlage.klageJournalpostid
                )
            ),
            brukersKlageMottattVedtaksinstans = lagretKlage.klageDato.toString(),
            ytelse = "OMS_OMP", //mockverdi - må avklares
            hjemler = listOf("FTRL_9_2"), //mockverdi - må avklares
            kommentar = "Kommentar fra veileder", // mockverdi - vurder å lage inputfelt ved resultat MEDHOLD
        )
    }

}

class KlageIkkeFunnetException(vedtakId: Long) :
    RuntimeException("Fant ingen klage for vedtakId $vedtakId")
