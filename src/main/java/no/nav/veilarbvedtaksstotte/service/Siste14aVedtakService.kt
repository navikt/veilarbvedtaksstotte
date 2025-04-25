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
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository
import no.nav.veilarbvedtaksstotte.utils.TimeUtils.toZonedDateTime
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class Siste14aVedtakService(
    private val kafkaProducerService: KafkaProducerService,
    private val vedtakRepository: VedtaksstotteRepository,
    private val arenaVedtakRepository: ArenaVedtakRepository,
    private val aktorOppslagClient: AktorOppslagClient,
) {

    val log: Logger = LoggerFactory.getLogger(Siste14aVedtakService::class.java)

    fun hentSiste14aVedtak(eksternBrukerId: EksternBrukerId): Siste14aVedtak? {
        val identer: BrukerIdenter = aktorOppslagClient.hentIdenter(eksternBrukerId)
        return hentSiste14aVedtakMedGrunnlag(identer).siste14aVedtak
    }

    fun republiserKafkaSiste14aVedtak(eksernBrukerId: EksternBrukerId) {
        val identer = hentIdenterMedDevSjekk(eksernBrukerId) ?: return // Prodlik q1 data er ikke tilgjengelig i dev

        val (siste14aVedtak) = hentSiste14aVedtakMedGrunnlag(identer)
        val fraArena = siste14aVedtak?.fraArena ?: false
        kafkaProducerService.sendSiste14aVedtak(siste14aVedtak)
        log.info("Siste 14a vedtak republisert basert på vedtak fra {}.", if (fraArena) "Arena" else "vedtaksstøtte")
    }

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

    private data class Siste14aVedtakMedGrunnlag(
        val siste14aVedtak: Siste14aVedtak?,
        val arenaVedtak: List<ArenaVedtak>
    )
}

