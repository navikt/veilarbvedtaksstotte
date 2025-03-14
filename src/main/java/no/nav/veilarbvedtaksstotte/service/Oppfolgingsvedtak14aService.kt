package no.nav.veilarbvedtaksstotte.service

import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.client.aktoroppslag.BrukerIdenter
import no.nav.common.types.identer.EksternBrukerId
import no.nav.common.utils.EnvironmentUtils.isDevelopment
import no.nav.veilarbvedtaksstotte.domain.vedtak.*
import no.nav.veilarbvedtaksstotte.domain.vedtak.ArenaVedtak.ArenaInnsatsgruppe
import no.nav.veilarbvedtaksstotte.repository.ArenaVedtakRepository
import no.nav.veilarbvedtaksstotte.repository.SisteOppfolgingPeriodeRepository
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository
import no.nav.veilarbvedtaksstotte.utils.SecureLog
import no.nav.veilarbvedtaksstotte.utils.TimeUtils.toZonedDateTime
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.time.ZoneId
import java.time.ZonedDateTime

@Service
class Oppfolgingsvedtak14aService @Autowired constructor(
    val transactor: TransactionTemplate,
    val kafkaProducerService: KafkaProducerService,
    val vedtakRepository: VedtaksstotteRepository,
    val arenaVedtakRepository: ArenaVedtakRepository,
    val aktorOppslagClient: AktorOppslagClient,
    val arenaVedtakService: ArenaVedtakService,
    val sisteOppfolgingPeriodeRepository: SisteOppfolgingPeriodeRepository,
) {

    val log: Logger = LoggerFactory.getLogger(Oppfolgingsvedtak14aService::class.java)

    fun hentGjeldende14aVedtak(brukerIdent: EksternBrukerId): Gjeldende14aVedtak? {
        val identer: BrukerIdenter = aktorOppslagClient.hentIdenter(brukerIdent)
        val innevarendeoppfolgingsperiode =
            sisteOppfolgingPeriodeRepository.hentInnevarendeOppfolgingsperiode(identer.aktorId) ?: return null
        val siste14aVedtak = siste14aVedtak(brukerIdent) ?: return null

        val erGjeldende: Boolean = sjekkOmVedtakErGjeldende(siste14aVedtak, innevarendeoppfolgingsperiode.startdato)

        return if (erGjeldende) {
            siste14aVedtak.toGjeldende14aVedtak() //PS 2025-03-12 dette objektet har ikke vedtakId :) Fortsettelse følger
        } else {
            null
        }
    }

    fun behandleGjeldende14aVedtak(arenaVedtak: ArenaVedtak) {
        val gjeldende14aVedtak = hentGjeldende14aVedtak(arenaVedtak.fnr)
        val aktorId = aktorOppslagClient.hentIdenter(arenaVedtak.fnr).aktorId

        if (gjeldende14aVedtak == null) {
            kafkaProducerService.sendGjeldende14aVedtak(aktorId, null)
            log.info("Fant ingen gjeldende § 14 a-vedtak for person. Sendte tombstone-melding.")
            SecureLog.secureLog.info(
                "Fant ingen gjeldende § 14 a-vedtak for person med Aktør-ID {}. Sendte tombstone-melding.",
                aktorId.get()
            )
            return
        }

        kafkaProducerService.sendGjeldende14aVedtak(aktorId, gjeldende14aVedtak.toGjeldende14aVedtakKafkaDTO())
        log.info("Mottat § 14 a-vedtak fra Arena er nytt gjeldende vedtak for person. Sender melding om gjeldende § 14 a-vedtak.")
        SecureLog.secureLog.info(
            "Mottat § 14 a-vedtak fra Arena er nytt gjeldende vedtak for person med Aktør-ID {}. Sender melding om gjeldende § 14 a-vedtak.",
            aktorId.get()
        )
    }

    fun siste14aVedtak(eksternBrukerId: EksternBrukerId): Siste14aVedtak? {
        val identer: BrukerIdenter = aktorOppslagClient.hentIdenter(eksternBrukerId)
        return siste14aVedtakMedKilder(identer).siste14aVedtak
    }

    fun siste14aVedtakFraArena(fnr: EksternBrukerId): ArenaVedtak? {
        val identer: BrukerIdenter = aktorOppslagClient.hentIdenter(fnr)
        val arenaVedtakListe = arenaVedtakRepository.hentVedtakListe(identer.historiskeFnr.plus(identer.fnr))
        val sisteArenaVedtak = finnSisteArenaVedtak(arenaVedtakListe)
        return sisteArenaVedtak
    }

    private data class Siste14aVedtakMedGrunnlag(
        val siste14aVedtak: Siste14aVedtak?,
        val arenaVedtak: List<ArenaVedtak>
    )

    private fun siste14aVedtakMedKilder(
        identer: BrukerIdenter
    ): Siste14aVedtakMedGrunnlag {
        val sisteVedtakNyLøsning: Vedtak? = vedtakRepository.hentSisteVedtak(identer.aktorId);
        val arenaVedtakListe = arenaVedtakRepository.hentVedtakListe(identer.historiskeFnr.plus(identer.fnr))


        if (sisteVedtakNyLøsning == null && arenaVedtakListe.isEmpty()) {
            return Siste14aVedtakMedGrunnlag(
                siste14aVedtak = null,
                arenaVedtak = arenaVedtakListe
            )
        }

        val sisteArenaVedtak = finnSisteArenaVedtak(arenaVedtakListe)

        // Siste vedtak fra denne løsningen
        if (
            (sisteVedtakNyLøsning != null && sisteArenaVedtak == null) ||
            (sisteVedtakNyLøsning != null && sisteArenaVedtak != null &&
                    sisteVedtakNyLøsning.vedtakFattet.isAfter(sisteArenaVedtak.beregnetFattetTidspunkt()))
        ) {
            return Siste14aVedtakMedGrunnlag(
                siste14aVedtak = Siste14aVedtak(
                    aktorId = identer.aktorId,
                    innsatsgruppe = sisteVedtakNyLøsning.innsatsgruppe,
                    hovedmal = HovedmalMedOkeDeltakelse.fraHovedmal(sisteVedtakNyLøsning.hovedmal),
                    fattetDato = toZonedDateTime(sisteVedtakNyLøsning.vedtakFattet),
                    fraArena = false,
                ),
                arenaVedtak = arenaVedtakListe
            )

            // Siste vedtak fra Arena
        } else if (sisteArenaVedtak != null) {
            return Siste14aVedtakMedGrunnlag(
                siste14aVedtak = Siste14aVedtak(
                    aktorId = identer.aktorId,
                    innsatsgruppe = ArenaInnsatsgruppe.tilInnsatsgruppe(sisteArenaVedtak.innsatsgruppe),
                    hovedmal = HovedmalMedOkeDeltakelse.fraArenaHovedmal(sisteArenaVedtak.hovedmal),
                    fattetDato = toZonedDateTime(sisteArenaVedtak.fraDato.atStartOfDay()),
                    fraArena = true,
                ),
                arenaVedtak = arenaVedtakListe
            )
        }

        // Ingen vedtak
        return Siste14aVedtakMedGrunnlag(
            siste14aVedtak = null,
            arenaVedtak = arenaVedtakListe
        )
    }

    private fun finnSisteArenaVedtak(arenaVedtakListe: List<ArenaVedtak>): ArenaVedtak? {
        return arenaVedtakListe.stream().max(Comparator.comparing(ArenaVedtak::fraDato)).orElse(null)
    }

    fun behandleEndringFraArena(arenaVedtak: ArenaVedtak) {
        // Feilhåndtering her skjer indirekte via feilhåndtering i Kafka-konsument, slik at meldinger vil bli behandlet
        // på nytt ved feil. Derfor gjøres idempotent oppdatering/lagring under i samme transaksjon som videre
        // oppdateringer i `settVedtakTilHistoriskOgSendSiste14aVedtakPaKafka`, slik at `harOppdatertVedtakFraArena`
        // er riktig selv ved ved retries i feilhåndteringen til Kafka-konsumenten.
        transactor.executeWithoutResult {
            // Idempotent oppdatering/lagring av vedtak fra Arena
            val harOppdatertVedtakFraArena = arenaVedtakService.behandleVedtakFraArena(arenaVedtak)


            // Oppdateringer som følger kunne vært gjort uavhengig av idempotent oppdatering/lagring over, og kunne
            // blitt utført uavhengig f.eks. i en scheduled task. Ved å sjekke om `arenaVedtak` har ført til en
            // oppdatering over, unngår man å behandle gamle meldinger ved ny last på topic.
            if (harOppdatertVedtakFraArena) {
                settVedtakTilHistoriskOgSendSiste14aVedtakPaKafka(arenaVedtak)
                behandleGjeldende14aVedtak(arenaVedtak)
            }
        }
    }

    private fun settVedtakTilHistoriskOgSendSiste14aVedtakPaKafka(arenaVedtak: ArenaVedtak) {
        val identer = hentIdenterMedDevSjekk(arenaVedtak.fnr) ?: return // Prodlik q1 data er ikke tilgjengelig i dev

        val (siste14aVedtak, arenaVedtakListe) = siste14aVedtakMedKilder(identer)
        val fraArena = siste14aVedtak?.fraArena ?: false

        // hindrer at vi republiserer siste14aVedtak dersom eldre meldinger skulle bli konsumert:
        val sisteFraArena = finnSisteArenaVedtak(arenaVedtakListe)
        val erSisteFraArena = fraArena && sisteFraArena?.hendelseId == arenaVedtak.hendelseId

        if (erSisteFraArena) {
            val siste14aVedtakNyLøsning: Vedtak? = vedtakRepository.hentGjeldendeVedtak(identer.aktorId.get())
            if (siste14aVedtakNyLøsning !== null) {
                setGjeldendeVedtakTilHistorisk(siste14aVedtakNyLøsning.id)
            }
            kafkaProducerService.sendSiste14aVedtak(siste14aVedtak)
        } else {
            log.info(
                """Publiserer ikke siste 14a vedtak basert på behandlet melding (fraArena=$fraArena, erSisteFraArena=$erSisteFraArena)
                |Behandlet melding har hendelseId=${arenaVedtak.hendelseId}
                |Siste melding har hendelseId=${sisteFraArena?.hendelseId}""".trimMargin()
            )
        }
    }

    private fun setGjeldendeVedtakTilHistorisk(vedtakId: Long) {
        log.info(
            "Setter vedtak med vedtakId=${vedtakId} fra vedtaksstøtte til historisk pga. nyere vedtak fra Arena"
        )
        vedtakRepository.settGjeldendeVedtakTilHistorisk(vedtakId)
    }

    fun republiserKafkaSiste14aVedtak(eksernBrukerId: EksternBrukerId) {
        val identer = hentIdenterMedDevSjekk(eksernBrukerId) ?: return // Prodlik q1 data er ikke tilgjengelig i dev

        val (siste14aVedtak) = siste14aVedtakMedKilder(identer)
        val fraArena = siste14aVedtak?.fraArena ?: false
        kafkaProducerService.sendSiste14aVedtak(siste14aVedtak)
        log.info("Siste 14a vedtak republisert basert på vedtak fra {}.", if (fraArena) "Arena" else "vedtaksstøtte")
    }

    private fun hentIdenterMedDevSjekk(brukerId: EksternBrukerId): BrukerIdenter? {
        return try {
            aktorOppslagClient.hentIdenter(brukerId)
        } catch (e: NullPointerException) {
            if (isDevelopment().orElse(false)) {
                log.info("Prøvde å hente prodlik bruker i dev. Returnerer null")
                return null
            } else throw e
        }
    }

    companion object {
        private val LANSERINGSDATO_VEILARBOPPFOLGING_OPPFOLGINGSPERIODE: ZonedDateTime =
            ZonedDateTime.of(2017, 12, 4, 0, 0, 0, 0, ZoneId.systemDefault())

        fun sjekkOmVedtakErGjeldende(
            siste14aVedtakForBruker: Siste14aVedtak,
            startDatoInnevarendeOppfolgingsperiode: ZonedDateTime
        ): Boolean {
            // 2025-02-18
            // Vi har oppdaget at vedtak fattet i Arena får "fattetDato" lik midnatt den dagen vedtaket ble fattet.
            // Derfor har vi valgt å innfør en "grace periode" på 4 døgn. Dvs. dersom vedtaket ble fattet etter
            // "oppfølgingsperiode startdato - 4 døgn", så anser vi det som gjeldende.
            val erVedtaketFattetIInnevarendeOppfolgingsperiodeMedGracePeriodePa4Dogn =
                siste14aVedtakForBruker.fattetDato.isAfter(startDatoInnevarendeOppfolgingsperiode.minusDays(4))
            val erVedtaketFattetForLanseringsdatoForVeilarboppfolging = siste14aVedtakForBruker.fattetDato
                .isBefore(LANSERINGSDATO_VEILARBOPPFOLGING_OPPFOLGINGSPERIODE)
            val erStartdatoForOppfolgingsperiodeLikLanseringsdatoForVeilarboppfolging =
                !startDatoInnevarendeOppfolgingsperiode
                    .isAfter(LANSERINGSDATO_VEILARBOPPFOLGING_OPPFOLGINGSPERIODE)

            return erVedtaketFattetIInnevarendeOppfolgingsperiodeMedGracePeriodePa4Dogn ||
                    (erVedtaketFattetForLanseringsdatoForVeilarboppfolging
                            && erStartdatoForOppfolgingsperiodeLikLanseringsdatoForVeilarboppfolging)
        }
    }
}
