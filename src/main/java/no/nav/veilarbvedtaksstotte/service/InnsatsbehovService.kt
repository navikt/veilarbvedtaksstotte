package no.nav.veilarbvedtaksstotte.service

import no.nav.common.types.identer.EksternBrukerId
import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.domain.BrukerIdenter
import no.nav.veilarbvedtaksstotte.domain.vedtak.ArenaVedtak
import no.nav.veilarbvedtaksstotte.domain.vedtak.ArenaVedtak.ArenaInnsatsgruppe
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsbehov
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsbehov.HovedmalMedOkeDeltakelse
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak
import no.nav.veilarbvedtaksstotte.repository.ArenaVedtakRepository
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate

@Service
class InnsatsbehovService(
    val transactor: TransactionTemplate,
    val kafkaProducerService: KafkaProducerService,
    val vedtakRepository: VedtaksstotteRepository,
    val arenaVedtakRepository: ArenaVedtakRepository,
    val authService: AuthService,
    val brukerIdentService: BrukerIdentService,
    val arenaVedtakService: ArenaVedtakService
) {

    val log = LoggerFactory.getLogger(InnsatsbehovService::class.java)

    fun sisteInnsatsbehov(fnr: Fnr): Innsatsbehov? {
        authService.sjekkTilgangTilBruker(fnr)
        val identer: BrukerIdenter = brukerIdentService.hentIdenter(fnr)
        return sisteInnsatsbehovMedKilder(identer).innsatsbehov
    }

    private data class InnsatsbehovMedGrunnlag(
        val innsatsbehov: Innsatsbehov?,
        val fraArena: Boolean,
        val arenaVedtak: List<ArenaVedtak>
    )

    private fun sisteInnsatsbehovMedKilder(identer: BrukerIdenter): InnsatsbehovMedGrunnlag {

        val sisteVedtak: Vedtak? = vedtakRepository.hentSisteVedtak(identer.aktorId.get())
        val arenaVedtakListe = arenaVedtakRepository.hentVedtakListe(identer.historiskeFnr.plus(identer.fnr))

        if (sisteVedtak == null && arenaVedtakListe.isEmpty()) {
            return InnsatsbehovMedGrunnlag(
                innsatsbehov = null,
                fraArena = false,
                arenaVedtak = arenaVedtakListe
            )
        }

        val sisteArenaVedtak = finnSisteArenaVedtak(arenaVedtakListe)

        // Siste vedtak fra denne løsningen
        if (
            (sisteVedtak != null && sisteArenaVedtak == null) ||
            (sisteVedtak != null && sisteArenaVedtak != null &&
                    sisteVedtak.vedtakFattet.isAfter(sisteArenaVedtak.beregnetFattetTidspunkt()))
        ) {
            return InnsatsbehovMedGrunnlag(
                innsatsbehov = Innsatsbehov(
                    aktorId = identer.aktorId,
                    innsatsgruppe = sisteVedtak.innsatsgruppe,
                    hovedmal = HovedmalMedOkeDeltakelse.fraHovedmal(sisteVedtak.hovedmal)
                ),
                fraArena = false,
                arenaVedtak = arenaVedtakListe
            )

            // Siste vedtak fra Arena
        } else if (sisteArenaVedtak != null) {
            return InnsatsbehovMedGrunnlag(
                innsatsbehov = Innsatsbehov(
                    aktorId = identer.aktorId,
                    innsatsgruppe = ArenaInnsatsgruppe.tilInnsatsgruppe(sisteArenaVedtak.innsatsgruppe),
                    hovedmal = HovedmalMedOkeDeltakelse.fraArenaHovedmal(sisteArenaVedtak.hovedmal)
                ),
                fraArena = true,
                arenaVedtak = arenaVedtakListe
            )
        }

        // Ingen vedtak
        return InnsatsbehovMedGrunnlag(
            innsatsbehov = null,
            fraArena = false,
            arenaVedtak = arenaVedtakListe
        )
    }

    private fun finnSisteArenaVedtak(arenaVedtakListe: List<ArenaVedtak>): ArenaVedtak? {
        return arenaVedtakListe.stream().max(Comparator.comparing(ArenaVedtak::fraDato)).orElse(null)
    }

    fun behandleEndringFraArena(arenaVedtak: ArenaVedtak) {
        // Idempotent oppdatering/lagring av vedtak fra Arena
        arenaVedtakService.behandleVedtakFraArena(arenaVedtak)

        // Oppdatering som følger kan gjøres uavhengig av idempotent oppdatering/lagring over. Derfor er ikke
        // oppdateringene i samme transaksjon, og følgende oppdatering kunne f.eks. også vært kjørt uavhengig i en
        // scheduled task. Feilhåndtering her skjer indirekte via feilhåndtering av Kafka-meldinger som vil bli
        // behandlet på nytt ved feil.
        oppdaterKafkaInnsatsbehov(arenaVedtak)
    }

    private fun oppdaterKafkaInnsatsbehov(arenaVedtak: ArenaVedtak) {
        val identer = brukerIdentService.hentIdenter(arenaVedtak.fnr)
        val (innsatsbehov, fraArena, arenaVedtakListe) = sisteInnsatsbehovMedKilder(identer)

        // hindrer at vi republiserer innsatsbehov dersom eldre meldinger skulle bli konsumert:
        val sisteFraArena = finnSisteArenaVedtak(arenaVedtakListe)
        val erSisteFraArena = fraArena && sisteFraArena?.hendelseId == arenaVedtak.hendelseId

        if (erSisteFraArena) {
            kafkaProducerService.sendInnsatsbehov(innsatsbehov)
        } else {
            log.info("""Publiserer ikke innsatsbehov basert på behandlet melding (fraArena=$fraArena, erSisteFraArena=$erSisteFraArena)
                |Behandlet melding har hendelseId=${arenaVedtak.hendelseId}
                |Siste melding har hendelseId=${sisteFraArena?.hendelseId}""".trimMargin())
        }
    }

    fun republiserKafkaInnsatsbehov(eksernBrukerId: EksternBrukerId) {
        val identer = brukerIdentService.hentIdenter(eksernBrukerId)
        val (innsatsbehov, fraArena) = sisteInnsatsbehovMedKilder(identer)
        kafkaProducerService.sendInnsatsbehov(innsatsbehov)
        log.info("Innsatsbehov republisert basert på vedtak fra {}.", if (fraArena) "Arena" else "vedtaksstøtte" )
    }
}
