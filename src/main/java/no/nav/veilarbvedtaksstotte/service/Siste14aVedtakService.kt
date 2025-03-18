package no.nav.veilarbvedtaksstotte.service

import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.client.aktoroppslag.BrukerIdenter
import no.nav.common.types.identer.EksternBrukerId
import no.nav.common.utils.EnvironmentUtils.isDevelopment
import no.nav.veilarbvedtaksstotte.domain.vedtak.ArenaVedtak
import no.nav.veilarbvedtaksstotte.domain.vedtak.ArenaVedtak.ArenaInnsatsgruppe
import no.nav.veilarbvedtaksstotte.domain.vedtak.HovedmalMedOkeDeltakelse
import no.nav.veilarbvedtaksstotte.domain.vedtak.Siste14aVedtak
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak
import no.nav.veilarbvedtaksstotte.repository.ArenaVedtakRepository
import no.nav.veilarbvedtaksstotte.repository.SisteOppfolgingPeriodeRepository
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository
import no.nav.veilarbvedtaksstotte.utils.TimeUtils.toZonedDateTime
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate

@Service
class Siste14aVedtakService(
    val transactor: TransactionTemplate,
    val kafkaProducerService: KafkaProducerService,
    val vedtakRepository: VedtaksstotteRepository,
    val arenaVedtakRepository: ArenaVedtakRepository,
    val aktorOppslagClient: AktorOppslagClient,
    val arenaVedtakService: ArenaVedtakService,
    val sisteOppfolgingPeriodeRepository: SisteOppfolgingPeriodeRepository
) {

    val log = LoggerFactory.getLogger(Siste14aVedtakService::class.java)

    fun siste14aVedtak(eksternBrukerId: EksternBrukerId): Siste14aVedtak? {
        val identer: BrukerIdenter = aktorOppslagClient.hentIdenter(eksternBrukerId)
        return hentSiste14aVedtakMedGrunnlag(identer).siste14aVedtak
    }

    private data class Siste14aVedtakMedGrunnlag(
        val siste14aVedtak: Siste14aVedtak?,
        val arenaVedtak: List<ArenaVedtak>
    )

    private fun hentSiste14aVedtakMedGrunnlag(
        identer: BrukerIdenter
    ): Siste14aVedtakMedGrunnlag {
        val nyesteVedtakFraNyLosning: Vedtak? = vedtakRepository.hentSisteVedtak(identer.aktorId)
        val arenaVedtakListe = arenaVedtakRepository.hentVedtakListe(identer.historiskeFnr.plus(identer.fnr))

        // Har ingen vedtak hverken fra ny løsning eller fra Arena
        if (nyesteVedtakFraNyLosning == null && arenaVedtakListe.isEmpty()) {
            return Siste14aVedtakMedGrunnlag(
                siste14aVedtak = null,
                arenaVedtak = arenaVedtakListe
            )
        }

        val nyesteVedtakFraArena = finnNyeste(arenaVedtakListe)

        // Siste vedtak fra denne løsningen
        if (
            (nyesteVedtakFraNyLosning != null && nyesteVedtakFraArena == null) ||
            (nyesteVedtakFraNyLosning != null && nyesteVedtakFraArena != null &&
                    nyesteVedtakFraNyLosning.vedtakFattet.isAfter(nyesteVedtakFraArena.beregnetFattetTidspunkt()))
        ) {
            return Siste14aVedtakMedGrunnlag(
                siste14aVedtak = Siste14aVedtak(
                    aktorId = identer.aktorId,
                    innsatsgruppe = nyesteVedtakFraNyLosning.innsatsgruppe,
                    hovedmal = HovedmalMedOkeDeltakelse.fraHovedmal(nyesteVedtakFraNyLosning.hovedmal),
                    fattetDato = toZonedDateTime(nyesteVedtakFraNyLosning.vedtakFattet),
                    fraArena = false,
                ),
                arenaVedtak = arenaVedtakListe
            )

            // Siste vedtak fra Arena
        } else if (nyesteVedtakFraArena != null) {
            return Siste14aVedtakMedGrunnlag(
                siste14aVedtak = Siste14aVedtak(
                    aktorId = identer.aktorId,
                    innsatsgruppe = ArenaInnsatsgruppe.tilInnsatsgruppe(nyesteVedtakFraArena.innsatsgruppe),
                    hovedmal = HovedmalMedOkeDeltakelse.fraArenaHovedmal(nyesteVedtakFraArena.hovedmal),
                    fattetDato = toZonedDateTime(nyesteVedtakFraArena.fraDato.atStartOfDay()),
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

    private fun finnNyeste(arenaVedtakListe: List<ArenaVedtak>): ArenaVedtak? {
        return arenaVedtakListe.stream().max(Comparator.comparing(ArenaVedtak::fraDato)).orElse(null)
    }

    fun behandleEndringFraArena(arenaVedtak: ArenaVedtak) {
        // Feilhåndtering her skjer indirekte via feilhåndtering i Kafka-konsument, slik at meldinger vil bli behandlet
        // på nytt ved feil. Derfor gjøres idempotent oppdatering/lagring under i samme transaksjon som videre
        // oppdateringer i `settVedtakTilHistoriskOgSendSiste14aVedtakPaKafka`, slik at `harOppdatertVedtakFraArena`
        // er riktig selv ved ved retries i feilhåndteringen til Kafka-konsumenten.
        transactor.executeWithoutResult {
            // Idempotent oppdatering/lagring av vedtak fra Arena
            val vedtakFraMeldingBleLagret = arenaVedtakService.behandleVedtakFraArena(arenaVedtak)

            // Oppdateringer som følger kunne vært gjort uavhengig av idempotent oppdatering/lagring over, og kunne
            // blitt utført uavhengig f.eks. i en scheduled task. Ved å sjekke om `arenaVedtak` har ført til en
            // oppdatering over, unngår man å behandle gamle meldinger ved ny last på topic.
            if (vedtakFraMeldingBleLagret) {
                val identer = hentIdenterMedDevSjekk(arenaVedtak.fnr)
                    ?: return@executeWithoutResult // Prodlik q1 data er ikke tilgjengelig i dev

                val siste14aVedtakMedGrunnlag = hentSiste14aVedtakMedGrunnlag(identer)
                val mottattArenaVedtakErNyesteVedtak =
                    sjekkOmVedtakErDetNyeste(arenaVedtak, siste14aVedtakMedGrunnlag)

                if (mottattArenaVedtakErNyesteVedtak) {
                    settEksisterendeGjeldende14aVedtakTilHistorisk(identer)
                    kafkaProducerService.sendSiste14aVedtak(siste14aVedtakMedGrunnlag.siste14aVedtak)
                } else {
                    log.info(
                        "Publiserer ikke § 14 a-vedtak fra Arena videre på egne topics. " +
                                "Årsak: det finnes vedtak (enten fra Arena eller ny løsning) for personen som er nyere."
                    )
                }
            }
        }
    }

    private fun sjekkOmVedtakErDetNyeste(
        vedtakSomSkalSjekkes: ArenaVedtak,
        siste14aVedtakMedGrunnlag: Siste14aVedtakMedGrunnlag
    ): Boolean {
        val nyesteVedtakErFraArena = siste14aVedtakMedGrunnlag.siste14aVedtak?.fraArena ?: false

        if (!nyesteVedtakErFraArena) {
            return false
        }

        val sisteFraArena = finnNyeste(siste14aVedtakMedGrunnlag.arenaVedtak)

        // `arenaVedtak` representerer en nylig mottatt melding om et § 14 a-vedtak fattet i Arena.
        // Dersom vi er inne i denne funksjonen betyr det implisitt at `arenaVedtak` har blitt lagret og overskrevet
        // et eventuelt eksisterende Arena-vedtak for den samme personen. Nå trenger vi å finne ut om det nylig lagrede
        // Arena-vedtaket også er det siste § 14 a-vedtaket for personen (av alle vedtak fra ny løsning og Arena).
        // Dersom det er det så må vi også publisere melding på siste § 14 a-vedtak topicet.
        return sisteFraArena?.hendelseId == vedtakSomSkalSjekkes.hendelseId
    }

    private fun settEksisterendeGjeldende14aVedtakTilHistorisk(identer: BrukerIdenter) {
        val siste14aVedtakNyLosning: Vedtak? = vedtakRepository.hentGjeldendeVedtak(identer.aktorId.get())
        if (siste14aVedtakNyLosning !== null) {
            setGjeldendeVedtakTilHistorisk(siste14aVedtakNyLosning.id)
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

        val (siste14aVedtak) = hentSiste14aVedtakMedGrunnlag(identer)
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
}
