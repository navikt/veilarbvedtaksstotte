package no.nav.veilarbvedtaksstotte.service

import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.client.aktoroppslag.BrukerIdenter
import no.nav.common.types.identer.EksternBrukerId
import no.nav.common.utils.EnvironmentUtils.isDevelopment
import no.nav.veilarbvedtaksstotte.config.KafkaProperties
import no.nav.veilarbvedtaksstotte.domain.vedtak.*
import no.nav.veilarbvedtaksstotte.domain.vedtak.ArenaVedtak.ArenaInnsatsgruppe
import no.nav.veilarbvedtaksstotte.repository.ArenaVedtakRepository
import no.nav.veilarbvedtaksstotte.repository.SisteOppfolgingPeriodeRepository
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository
import no.nav.veilarbvedtaksstotte.service.Gjeldende14aVedtakService.Companion.sjekkOmVedtakErGjeldende
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
    val sisteOppfolgingPeriodeRepository: SisteOppfolgingPeriodeRepository,
    private val kafkaProperties: KafkaProperties
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
        val alleArenaVedtakForPersonen = arenaVedtakRepository.hentVedtakListe(identer.historiskeFnr.plus(identer.fnr))
        val nyesteVedtakFraNyLosning: Vedtak? = vedtakRepository.hentSisteVedtak(identer.aktorId)
        val nyesteVedtakFraArena = finnNyeste(alleArenaVedtakForPersonen)

        // Det nyeste vedtaket for personen kommer fra denne løsningen
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
                arenaVedtak = alleArenaVedtakForPersonen
            )
        }

        // Det nyeste vedtaket for personen kommer fra Arena
        if (nyesteVedtakFraArena != null) {
            return Siste14aVedtakMedGrunnlag(
                siste14aVedtak = Siste14aVedtak(
                    aktorId = identer.aktorId,
                    innsatsgruppe = ArenaInnsatsgruppe.tilInnsatsgruppe(nyesteVedtakFraArena.innsatsgruppe),
                    hovedmal = HovedmalMedOkeDeltakelse.fraArenaHovedmal(nyesteVedtakFraArena.hovedmal),
                    fattetDato = toZonedDateTime(nyesteVedtakFraArena.fraDato.atStartOfDay()),
                    fraArena = true,
                ),
                arenaVedtak = alleArenaVedtakForPersonen
            )
        }

        return Siste14aVedtakMedGrunnlag(
            siste14aVedtak = null,
            arenaVedtak = emptyList()
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
                publiserEndringer(arenaVedtak)
            }
        }
    }

    private fun publiserEndringer(arenaVedtak: ArenaVedtak) {
        val identer = hentIdenterMedDevSjekk(arenaVedtak.fnr)
            ?: return // Prodlik q1 data er ikke tilgjengelig i dev

        val siste14aVedtakMedGrunnlag = hentSiste14aVedtakMedGrunnlag(identer)

        // `arenaVedtak` representerer en nylig mottatt melding om et § 14 a-vedtak fattet i Arena.
        // Dersom vi er inne i denne funksjonen betyr det implisitt at `arenaVedtak` har blitt lagret og overskrevet
        // et eventuelt eksisterende Arena-vedtak for den samme personen. Nå trenger vi å finne ut om det nylig lagrede
        // Arena-vedtaket også er det siste § 14 a-vedtaket for personen (av alle vedtak fra ny løsning og Arena).
        // Dersom det er det så må vi også publisere melding på siste § 14 a-vedtak topicet.
        val erMottattArenaVedtakNyesteVedtak =
            sjekkOmVedtakErDetNyeste(
                vedtakSomSkalSjekkes = arenaVedtak,
                siste14aVedtakMedGrunnlag = siste14aVedtakMedGrunnlag
            )
        val erMottattArenaVedtakGjeldende =
            sjekkOmVedtakErGjeldende(identer = identer, siste14aVedtakMedGrunnlag = siste14aVedtakMedGrunnlag)

        if (erMottattArenaVedtakNyesteVedtak) {
            settEksisterendeGjeldende14aVedtakTilHistorisk(identer)
            kafkaProducerService.sendSiste14aVedtak(siste14aVedtakMedGrunnlag.siste14aVedtak)
            log.info("Videresendte § 14 a-vedtak fra Arena på {}.", kafkaProperties.siste14aVedtakTopic)
        } else {
            log.info(
                "Videresender ikke § 14 a-vedtak fra Arena på {}. Årsak: det finnes vedtak for personen som er nyere.",
                kafkaProperties.siste14aVedtakTopic
            )
        }

        if (erMottattArenaVedtakGjeldende) {
            kafkaProducerService.sendGjeldende14aVedtak(
                identer.aktorId,
                siste14aVedtakMedGrunnlag.siste14aVedtak?.toGjeldende14aVedtakKafkaDTO()
            )
            log.info("Videresendte § 14 a-vedtak fra Arena på {}.", kafkaProperties.gjeldende14aVedtakTopic)
        } else {
            log.info(
                "Videresender ikke § 14 a-vedtak fra Arena på {}. Årsak: vedtaket er ikke gjeldende.",
                kafkaProperties.gjeldende14aVedtakTopic
            )
        }
    }

    private fun sjekkOmVedtakErGjeldende(
        identer: BrukerIdenter,
        siste14aVedtakMedGrunnlag: Siste14aVedtakMedGrunnlag
    ): Boolean {
        val innevaerendeOppfolgingsperiode =
            sisteOppfolgingPeriodeRepository.hentInnevaerendeOppfolgingsperiode(identer.aktorId)

        if (siste14aVedtakMedGrunnlag.siste14aVedtak == null || innevaerendeOppfolgingsperiode == null) {
            return false
        }

        return sjekkOmVedtakErGjeldende(
            siste14aVedtakMedGrunnlag.siste14aVedtak,
            innevaerendeOppfolgingsperiode.startdato
        )
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

        return sisteFraArena?.hendelseId == vedtakSomSkalSjekkes.hendelseId
    }

    private fun settEksisterendeGjeldende14aVedtakTilHistorisk(identer: BrukerIdenter) {
        val siste14aVedtakNyLosning: Vedtak? = vedtakRepository.hentGjeldendeVedtak(identer.aktorId.get())
        if (siste14aVedtakNyLosning !== null) {
            log.info(
                "Setter vedtak med vedtakId=${siste14aVedtakNyLosning.id} fra vedtaksstøtte til historisk pga. nyere vedtak fra Arena"
            )
            vedtakRepository.settGjeldendeVedtakTilHistorisk(siste14aVedtakNyLosning.id)
        }
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
