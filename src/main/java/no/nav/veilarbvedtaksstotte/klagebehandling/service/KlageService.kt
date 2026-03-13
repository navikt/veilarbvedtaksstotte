package no.nav.veilarbvedtaksstotte.klagebehandling.service

import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.poao_tilgang.client.NorskIdent
import no.nav.veilarbvedtaksstotte.client.dokarkiv.SafClient
import no.nav.veilarbvedtaksstotte.client.dokarkiv.dto.JournalpostGraphqlResponse
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
import java.time.LocalDate

@Service
class KlageService(
    @param:Autowired private val klageRepository: KlageRepository,
    @param:Autowired private val kabalClient: KabalClient,
    @param:Autowired private val vedtakRepository: VedtaksstotteRepository,
    @param:Autowired private val safClient: SafClient,
    @param:Autowired private val aktorOppslagClient: AktorOppslagClient
) {

    val logger: Logger = LoggerFactory.getLogger(KlageService::class.java)

    fun prosesserKlagebehandlingHendelse(
        forrigeTilstand: KlagebehandlingTilstand = IngenKlagebehandling,
        hendelse: KlagebehandlingHendelse
    ): KlagebehandlingHendelseResultat {
        return when (hendelse) {
            is KlagebehandlingHendelse.StartKlagebehandling -> {
                if (forrigeTilstand !is IngenKlagebehandling) {
                    throw UlovligForrigeTilstandException()
                }

                val data = hendelse.data
                hendelse.validator.valider(data)
                klageRepository.upsertKlagebehandling(KlageBehandling(klageInitiellData = data))
                KlagebehandlingHendelseResultat.Ok(tilstand = KlagebehandlingStartet(data = data))
            }

            is KlagebehandlingHendelse.OppdaterFormkrav -> {
                if (forrigeTilstand !is KlagebehandlingStartet) {
                    throw UlovligForrigeTilstandException()
                }

                TODO()
            }

            is KlagebehandlingHendelse.AvvisKlage -> {
                if (forrigeTilstand !is KlagebehandlingStartet) {
                    throw UlovligForrigeTilstandException()
                }

                TODO()
            }

            is KlagebehandlingHendelse.FullførAvvisning -> {
                if (forrigeTilstand !is KlageAvvist) {
                    throw UlovligForrigeTilstandException()
                }

                TODO()
            }
        }
    }

    fun startNyKlagebehandling(klagebehandlingGenerellData: KlageInitiellData) {
        val vedtakSupplier = { vedtakId: Long -> vedtakRepository.hentSendtVedtak(vedtakId) }
        val journalpostSupplier = { journalpostId: String -> safClient.hentJournalpost(journalpostId) }
        val aktorIdSupplier = { norskIdent: NorskIdent -> aktorOppslagClient.hentAktorId(Fnr.of(norskIdent)) }

//        prosesserKlagebehandlingHendelse()
        klageRepository.upsertKlagebehandling(KlageBehandling(klageInitiellData = klagebehandlingGenerellData))
    }

    fun oppdaterFormkrav(vedtakId: Long, klagebehandlingFormkravData: KlageFormkravData) {
        val formkravKlagefristOppfylt = klagebehandlingFormkravData.formkravKlagefristOpprettholdt == FormkravSvar.JA
                || (klagebehandlingFormkravData.formkravKlagefristUnntak != null && klagebehandlingFormkravData.formkravKlagefristUnntak != FormkravKlagefristUnntakSvar.NEI)

        val alleFormkravOppfylt =
            klagebehandlingFormkravData.formkravSignert == FormkravSvar.JA
                    && klagebehandlingFormkravData.formkravPart == FormkravSvar.JA
                    && klagebehandlingFormkravData.formkravKonkret == FormkravSvar.JA
                    && formkravKlagefristOppfylt

        val formkravOppfyltString =
            if (alleFormkravOppfylt) FormkravOppfylt.OPPFYLT else FormkravOppfylt.IKKE_OPPFYLT

        klageRepository.updateFormkrav(
            vedtakId,
            klagebehandlingFormkravData,
            formkravOppfyltString
        )

        if (!alleFormkravOppfylt) {
            klageRepository.updateResultat(
                vedtakId,
                Resultat.AVVIST,
                klagebehandlingFormkravData.formkravBegrunnelseIntern
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

sealed interface Validator<T> {
    fun valider(data: T): ValideringResultat
}

/**
 * Valideringsregler for [KlageInitiellData]:
 *
 * 1. det må faktisk eksistere et vedtak gitt ved [KlageInitiellData.vedtakId]
 * 2. vedtaket gitt ved [KlageInitiellData.vedtakId] må faktisk tilhøre personen gitt ved [KlageInitiellData.norskIdent]
 * 3. klagedato gitt ved [KlageInitiellData.klageDato] kan ikke være frem i tid
 * 4. klagedato gitt ved [KlageInitiellData.klageDato] må være etter datoen vedtaket som det klages på ble fattet
 * 5. det må faktisk eksistere en journalpost gitt ved [KlageInitiellData.klageJournalpostid]
 */
data class GenerellDataValidator(
    val vedtakSupplier: (vedtakId: Long) -> Vedtak?,
    val aktorIdSupplier: (norskIdent: NorskIdent) -> AktorId,
    val journalpostSupplier: (journalpostId: String) -> JournalpostGraphqlResponse?
) : Validator<KlageInitiellData> {
    override fun valider(data: KlageInitiellData): ValideringResultat {
        val vedtak = vedtakSupplier(data.vedtakId)
        val aktorId = aktorIdSupplier(data.norskIdent)
        val journalpost = journalpostSupplier(data.klageJournalpostid)

        return when {
            vedtak == null -> {
                ValideringResultat.Feil("Ingen vedtak med oppgitt id eksisterer.")
            }

            vedtak.aktorId != aktorId.get() -> {
                ValideringResultat.Feil("Oppgitt vedtak tilhører ikke oppgitt person.")
            }

            data.klageDato.isAfter(LocalDate.now()) -> {
                ValideringResultat.Feil("Klagedato kan ikke være frem i tid.")
            }

            data.klageDato.isBefore(vedtak.vedtakFattet.toLocalDate()) -> {
                ValideringResultat.Feil("Klagedato kan ikke være før vedtak fattet dato.")
            }

            journalpost == null -> {
                ValideringResultat.Feil("Ingen journalpost med oppgitt id eksisterer.")
            }

            else -> ValideringResultat.Ok
        }
    }
}

sealed interface KlagebehandlingHendelseResultat {
    data class Ok(val tilstand: KlagebehandlingTilstand) : KlagebehandlingHendelseResultat
    data object Feil : KlagebehandlingHendelseResultat
}

sealed interface KlagebehandlingTilstand

data object IngenKlagebehandling : KlagebehandlingTilstand
data class KlagebehandlingStartet(val data: KlageInitiellData) : KlagebehandlingTilstand
data class KlageAvvist(val data: KlageFormkravData) : KlagebehandlingTilstand

sealed interface KlagebehandlingHendelse {
    data class StartKlagebehandling(val data: KlageInitiellData, val validator: Validator<KlageInitiellData>) :
        KlagebehandlingHendelse

    data class OppdaterFormkrav(val data: KlageFormkravData, val validator: Validator<KlageFormkravData>) :
        KlagebehandlingHendelse

    data class AvvisKlage(val data: KlageFormkravData, val validator: Validator<KlageFormkravData>) :
        KlagebehandlingHendelse

    data class FullførAvvisning(val data: KlageInitiellData, val validator: Validator<KlageInitiellData>) :
        KlagebehandlingHendelse
}

sealed interface ValideringResultat {
    data object Ok : ValideringResultat
    data class Feil(val årsak: String) : ValideringResultat
}

data class UlovligForrigeTilstandException(val melding: String? = null) : RuntimeException(melding)
