package no.nav.veilarbvedtaksstotte.klagebehandling.service

import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.client.dokarkiv.SafClient
import no.nav.veilarbvedtaksstotte.client.dokarkiv.dto.Journalpost
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak
import no.nav.veilarbvedtaksstotte.klagebehandling.client.*
import no.nav.veilarbvedtaksstotte.klagebehandling.controller.FormkravKlagefristUnntakSvar
import no.nav.veilarbvedtaksstotte.klagebehandling.controller.FormkravSvar
import no.nav.veilarbvedtaksstotte.klagebehandling.controller.HentKlageRequest
import no.nav.veilarbvedtaksstotte.klagebehandling.domene.*
import no.nav.veilarbvedtaksstotte.klagebehandling.repository.KlagebehandlingRepository
import no.nav.veilarbvedtaksstotte.klagebehandling.service.Feil.Årsak.*
import no.nav.veilarbvedtaksstotte.klagebehandling.service.KlageService.Mapper.tilKabalDTO
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDate.now
import java.util.*
import kotlin.jvm.optionals.getOrNull

@Service
class KlageService(
    @param:Autowired private val klagebehandlingRepository: KlagebehandlingRepository,
    @param:Autowired private val vedtakRepository: VedtaksstotteRepository,
    @param:Autowired private val safClient: SafClient,
    @param:Autowired private val aktorOppslagClient: AktorOppslagClient,
    @param:Autowired private val kabalClient: KabalClient,
) {

    val logger: Logger = LoggerFactory.getLogger(KlageService::class.java)

    fun hentKlage(vedtakId: Long): KlagebehandlingHendelseResultat<Klagebehandling> {
        return try {
            klagebehandlingRepository.hentKlageBehandling(vedtakId)
                ?.let { Ok(data = it) }
                ?: Feil(årsak = KLAGE_IKKE_FUNNET)
        } catch (_: RuntimeException) {
            Feil(årsak = UKJENT_FEIL)
        }
    }

    fun sendKlageTilKabal(hentKlageRequest: HentKlageRequest): KlagebehandlingHendelseResultat<Unit> {
        val lagretKlage = klagebehandlingRepository.hentKlageBehandling(hentKlageRequest.vedtakId)
            ?: return Feil(årsak = KLAGE_IKKE_FUNNET)
        val lagretVedtak = vedtakRepository.hentVedtak(hentKlageRequest.vedtakId)
        val kabalDto = tilKabalDTO(lagretKlage, lagretVedtak)

        return try {
            kabalClient.sendKlageTilKabal(kabalDto)
            klagebehandlingRepository.updateStatus(hentKlageRequest.vedtakId, Status.SENDT_TIL_KABAL)
            logger.info("Klage sendt til Kabal og status oppdatert for vedtakId ${hentKlageRequest.vedtakId}")
            Ok(Unit)
        } catch (e: Exception) {
            logger.error("Feil ved sending av klage til Kabal for vedtakId ${hentKlageRequest.vedtakId}", e)
            Feil(årsak = UKJENT_FEIL)
        }
    }

    fun startNyKlagebehandling(data: KlageInitiellData): KlagebehandlingHendelseResultat<KlagebehandlingId> {
        val nåværendeTilstand = hentNåværendeKlagebehandlingTilstand(vedtakId = data.vedtakId)
        val påklagetVedtak = vedtakRepository.hentSendtVedtak(data.vedtakId).getOrNull()?.let {
            KlagebehandlingStartGrunnlag.PåklagetVedtak(
                vedtak = it,
                vedtakFnr = aktorOppslagClient.hentFnr(AktorId.of(it.aktorId))
            )
        }
        val klagebrevJournalpost = safClient.hentJournalpost(data.klageJournalpostid).data.journalpost
        val grunnlag = KlagebehandlingStartGrunnlag(
            påklagetVedtak = påklagetVedtak,
            journalpost = klagebrevJournalpost,
            idag = now()
        )
        val resultat = prosesserKlagebehandlingHendelse(
            forrigeTilstand = nåværendeTilstand,
            hendelse = StartKlagebehandling(data = data, grunnlag = grunnlag),
        )

        return when (resultat) {
            // TODO Ikkje bruke upsert her då det bryt med validering?
            is Ok -> {
                val klagebehandlingId = klagebehandlingRepository.upsertKlagebehandling(
                    Klagebehandling(
                        klageInitiellData = data,
                        klageStatus = Status.UTKAST
                    )
                )
                Ok(data = klagebehandlingId)
            }

            is Feil -> resultat
        }
    }

    fun oppdaterFormkrav(vedtakId: Long, data: KlageFormkravData): KlagebehandlingHendelseResultat<Unit> {
        val nåværendeTilstand = hentNåværendeKlagebehandlingTilstand(vedtakId = vedtakId)
        val resultat = prosesserKlagebehandlingHendelse(
            nåværendeTilstand, OppdaterFormkrav(data = data)
        )

        if (resultat is Ok<Unit>) {
            klagebehandlingRepository.updateFormkrav(
                vedtakId,
                KlageFormkravData(
                    formkravSignert = data.formkravSignert,
                    formkravPart = data.formkravPart,
                    formkravKonkret = data.formkravKonkret,
                    formkravKlagefristOpprettholdt = data.formkravKlagefristOpprettholdt,
                    formkravKlagefristUnntak = data.formkravKlagefristUnntak,
                    formkravBegrunnelseIntern = data.formkravBegrunnelseIntern,
                    formkravBegrunnelseBrev = data.formkravBegrunnelseBrev,
                ),
                FormkravOppfylt.IKKE_OPPFYLT
            )
        }

        return resultat
    }

    fun avvisKlage(vedtakId: Long, data: KlageFormkravData): KlagebehandlingHendelseResultat<Unit> {
        val nåværendeTilstand = hentNåværendeKlagebehandlingTilstand(vedtakId = vedtakId)
        val resultat = prosesserKlagebehandlingHendelse(
            nåværendeTilstand, AvvisKlage(data = data)
        )

        if (resultat is Ok<Unit>) {
            klagebehandlingRepository.updateFormkrav(
                vedtakId = vedtakId,
                formkravOppfylt = FormkravOppfylt.IKKE_OPPFYLT,
                formkrav = KlageFormkravData(
                    formkravSignert = data.formkravSignert,
                    formkravPart = data.formkravPart,
                    formkravKonkret = data.formkravKonkret,
                    formkravKlagefristOpprettholdt = data.formkravKlagefristOpprettholdt,
                    formkravKlagefristUnntak = data.formkravKlagefristUnntak,
                    formkravBegrunnelseBrev = data.formkravBegrunnelseBrev,
                    formkravBegrunnelseIntern = null
                )
            )
            klagebehandlingRepository.updateStatus(vedtakId, Status.AVVIST)
        }

        return resultat
    }

    fun fullførAvvisning(
        vedtakId: Long,
        klageAvvisningData: KlageAvvisningData
    ): KlagebehandlingHendelseResultat<Unit> {
        val nåværendeTilstand = hentNåværendeKlagebehandlingTilstand(vedtakId = vedtakId)
        val grunnlag =
            KlagebehandlingFullførGrunnlag(safClient.hentJournalpost(klageAvvisningData.avvisningsbrevJournalpostId).data.journalpost)
        val resultat = prosesserKlagebehandlingHendelse(
            nåværendeTilstand, FullførAvvisning(data = klageAvvisningData, grunnlag = grunnlag)
        )

        if (resultat is Ok<Unit>) {
            klagebehandlingRepository.updateResultat(vedtakId, Resultat.AVVIST)
            klagebehandlingRepository.updateStatus(vedtakId, Status.FERDIGSTILT)
        }

        return resultat
    }

    private fun hentNåværendeKlagebehandlingTilstand(vedtakId: Long): KlagebehandlingTilstand {
        val eksisterendeTilstand = klagebehandlingRepository.hentKlageBehandling(vedtakId) ?: return KlagebehandlingTilstandIngen

        return when (eksisterendeTilstand.klageStatus) {
            Status.UTKAST -> KlagebehandlingTilstandStartet(data = eksisterendeTilstand)
            Status.AVVIST -> KlagebehandlingTilstandAvvist(data = eksisterendeTilstand)
            Status.FERDIGSTILT -> KlagebehandlingTilstandFullført(data = eksisterendeTilstand)
            else -> KlagebehandlingTilstandUkjent
        }
    }

    // TODO: "Tvinge" alle handlers her til å forhalde seg til same format
    fun prosesserKlagebehandlingHendelse(
        forrigeTilstand: KlagebehandlingTilstand = KlagebehandlingTilstandIngen,
        hendelse: KlagebehandlingHendelse,
    ): KlagebehandlingHendelseResultat<Unit> {
        return when (hendelse) {
            is StartKlagebehandling ->
                prosesserStartKlagebehandlingHendelse(forrigeTilstand, hendelse)

            is OppdaterFormkrav ->
                prosesserOppdaterFormkravHendelse(forrigeTilstand, hendelse)

            is AvvisKlage ->
                prosesserAvvisKlageHendelse(forrigeTilstand, hendelse)

            is FullførAvvisning ->
                prosseserFullførAvvisningHendelse(forrigeTilstand, hendelse)
        }
    }

    /**
     * Sjekkar om vi kan starte klagebehandling for eit gitt vedtak
     */
    private fun prosesserStartKlagebehandlingHendelse(
        forrigeTilstand: KlagebehandlingTilstand,
        hendelse: StartKlagebehandling
    ): KlagebehandlingHendelseResultat<Unit> {
        if (forrigeTilstand !is KlagebehandlingTilstandIngen) return Feil(årsak = ULOVLIG_NÅVÆRENDE_KLAGEBEHANDLING_TILSTAND)

        val grunnlag = hendelse.grunnlag
        val data = hendelse.data

        val påklagetVedtak = grunnlag.påklagetVedtak
            ?: return Feil(årsak = PÅKLAGET_VEDTAK_IKKE_FUNNET)
        if (påklagetVedtak.vedtakFnr.get() != data.norskIdent) return Feil(årsak = PÅKLAGET_VEDTAK_TILHØRER_IKKE_BRUKER)
        if (grunnlag.journalpost == null) return Feil(årsak = KLAGEBREV_JOURNALPOST_IKKE_FUNNET)
        if (grunnlag.journalpost.bruker.id != data.norskIdent) return Feil(årsak = KLAGEBREV_JOURNALPOST_TILHØRER_IKKE_BRUKER)
        if (data.klageDato.isAfter(grunnlag.idag)) return Feil(årsak = KLAGEDATO_ER_FREM_I_TID)
        if (data.klageDato.isBefore(påklagetVedtak.vedtak.vedtakFattet.toLocalDate())) return Feil(årsak = KLAGEDATO_ER_FØR_VEDTAK_FATTET_DATO)

        return Ok(Unit)
    }

    private fun prosesserOppdaterFormkravHendelse(
        forrigeTilstand: KlagebehandlingTilstand,
        hendelse: OppdaterFormkrav
    ): KlagebehandlingHendelseResultat<Unit> {
        if (forrigeTilstand !is KlagebehandlingTilstandStartet) {
            return Feil(årsak = ULOVLIG_NÅVÆRENDE_KLAGEBEHANDLING_TILSTAND)
        }

        val data = hendelse.data

        if (data.formkravBegrunnelseBrev != null) {
            // Dersom begrunnelse er satt må minst et av kravene være "ikke oppfylt"
            if (FormkravSvar.JA == data.formkravSignert
                && FormkravSvar.JA == data.formkravPart
                && FormkravSvar.JA == data.formkravKonkret
                && (FormkravSvar.JA == data.formkravKlagefristOpprettholdt ||
                        (FormkravSvar.NEI == data.formkravKlagefristOpprettholdt && null != data.formkravKlagefristUnntak && FormkravKlagefristUnntakSvar.NEI != data.formkravKlagefristUnntak))
            ) {
                return Feil(årsak = FORMKRAV_BEGRUNNELSE_SATT_UTEN_RIKTIGE_KRITERIER)
            }
        }

        return Ok(Unit)
    }

    private fun prosesserAvvisKlageHendelse(
        forrigeTilstand: KlagebehandlingTilstand,
        hendelse: AvvisKlage
    ): KlagebehandlingHendelseResultat<Unit> {
        if (forrigeTilstand !is KlagebehandlingTilstandStartet) {
            return Feil(årsak = ULOVLIG_NÅVÆRENDE_KLAGEBEHANDLING_TILSTAND)
        }

        val data = hendelse.data
        if (data.formkravSignert == FormkravSvar.JA
            && data.formkravPart == FormkravSvar.JA
            && data.formkravKonkret == FormkravSvar.JA
            && (data.formkravKlagefristOpprettholdt == FormkravSvar.JA
                    || data.formkravKlagefristUnntak == FormkravKlagefristUnntakSvar.JA_SAERLIGE_GRUNNER
                    || data.formkravKlagefristUnntak == FormkravKlagefristUnntakSvar.JA_KLAGER_KAN_IKKE_LASTES)
        ) {
            return Feil(årsak = KAN_IKKE_AVVISE_KLAGE_NÅR_FORMKRAV_OPPFYLT)
        }
        if (
            null == data.formkravSignert ||
            null == data.formkravPart ||
            null == data.formkravKonkret ||
            null == data.formkravKlagefristOpprettholdt ||
            null == data.formkravBegrunnelseIntern
        ) {
            return Feil(årsak = ALLE_FORMKRAV_MÅ_VÆRE_SATT)
        }

        if (FormkravSvar.NEI == data.formkravKlagefristOpprettholdt &&
            null == data.formkravKlagefristUnntak
        ) {
            return Feil(årsak = FRIST_IKKE_OPPRETTHOLDT_KREVER_UNNTAK_SATT)
        }

        if (data.formkravBegrunnelseBrev.isNullOrEmpty()) {
            return Feil(årsak = FORMKRAV_BEGRUNNELSE_MANGLER)
        }

        return Ok(Unit)
    }

    private fun prosseserFullførAvvisningHendelse(
        forrigeTilstand: KlagebehandlingTilstand,
        hendelse: FullførAvvisning
    ): KlagebehandlingHendelseResultat<Unit> {
        if (forrigeTilstand !is KlagebehandlingTilstandAvvist) {
            return Feil(årsak = ULOVLIG_NÅVÆRENDE_KLAGEBEHANDLING_TILSTAND)
        }

        val grunnlag = hendelse.grunnlag

        if (grunnlag.avvisningsbrevJournalpost == null) {
            return Feil(årsak = AVVISNINGSBREV_JOURNALPOST_IKKE_FUNNET)
        }

        return Ok(Unit)
    }

    object Mapper {
        fun tilKabalDTO(lagretKlage: Klagebehandling, lagretVedtak: Vedtak): KabalDTO {
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
}

sealed interface KlagebehandlingHendelseResultat<out T>
data class Ok<T>(val data: T) : KlagebehandlingHendelseResultat<T>
data class Feil(val årsak: Årsak = UKJENT_FEIL) : KlagebehandlingHendelseResultat<Nothing> {
    enum class Årsak {
        ULOVLIG_NÅVÆRENDE_KLAGEBEHANDLING_TILSTAND,
        PÅKLAGET_VEDTAK_IKKE_FUNNET,
        PÅKLAGET_VEDTAK_TILHØRER_IKKE_BRUKER,
        KLAGEBREV_JOURNALPOST_IKKE_FUNNET,
        KLAGEBREV_JOURNALPOST_TILHØRER_IKKE_BRUKER,
        KLAGEDATO_ER_FREM_I_TID,
        KLAGEDATO_ER_FØR_VEDTAK_FATTET_DATO,
        FORMKRAV_BEGRUNNELSE_SATT_UTEN_RIKTIGE_KRITERIER,
        FORMKRAV_BEGRUNNELSE_MANGLER,
        ALLE_FORMKRAV_MÅ_VÆRE_SATT,
        FRIST_IKKE_OPPRETTHOLDT_KREVER_UNNTAK_SATT,
        KAN_IKKE_AVVISE_KLAGE_NÅR_FORMKRAV_OPPFYLT,
        AVVISNINGSBREV_JOURNALPOST_IKKE_FUNNET,
        KLAGE_IKKE_FUNNET,
        UKJENT_FEIL
    }
}

// Tilstander
sealed interface KlagebehandlingTilstand
data object KlagebehandlingTilstandIngen : KlagebehandlingTilstand
data class KlagebehandlingTilstandStartet(val data: Klagebehandling) : KlagebehandlingTilstand
data class KlagebehandlingTilstandAvvist(val data: Klagebehandling) : KlagebehandlingTilstand
data class KlagebehandlingTilstandFullført(val data: Klagebehandling) : KlagebehandlingTilstand
data object KlagebehandlingTilstandUkjent : KlagebehandlingTilstand

// Hendelser
sealed interface KlagebehandlingHendelse
data class StartKlagebehandling(val data: KlageInitiellData, val grunnlag: KlagebehandlingStartGrunnlag) :
    KlagebehandlingHendelse

data class OppdaterFormkrav(val data: KlageFormkravData) : KlagebehandlingHendelse
data class AvvisKlage(val data: KlageFormkravData) : KlagebehandlingHendelse
data class FullførAvvisning(val data: KlageAvvisningData, val grunnlag: KlagebehandlingFullførGrunnlag) :
    KlagebehandlingHendelse

data class KlagebehandlingStartGrunnlag(
    val påklagetVedtak: PåklagetVedtak?,
    val journalpost: Journalpost?,
    val idag: LocalDate
) {
    data class PåklagetVedtak(
        val vedtak: Vedtak,
        val vedtakFnr: Fnr
    )
}

data class KlagebehandlingFullførGrunnlag(
    val avvisningsbrevJournalpost: Journalpost?
)
