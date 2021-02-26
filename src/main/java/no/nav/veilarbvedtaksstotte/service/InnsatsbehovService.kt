package no.nav.veilarbvedtaksstotte.service

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.OppfolgingPeriodeDTO
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.VeilarboppfolgingClient
import no.nav.veilarbvedtaksstotte.domain.BrukerIdenter
import no.nav.veilarbvedtaksstotte.domain.vedtak.ArenaVedtak
import no.nav.veilarbvedtaksstotte.domain.vedtak.ArenaVedtak.ArenaInnsatsgruppe
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsbehov
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsbehov.HovedmalMedOkeDeltakelse
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak
import no.nav.veilarbvedtaksstotte.kafka.KafkaProducer
import no.nav.veilarbvedtaksstotte.kafka.dto.KafkaAvsluttOppfolging
import no.nav.veilarbvedtaksstotte.repository.ArenaVedtakRepository
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository
import no.nav.veilarbvedtaksstotte.utils.OppfolgingUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.util.*

@Service
class InnsatsbehovService(
    val authService: AuthService,
    val brukerIdentService: BrukerIdentService,
    val vedtakRepository: VedtaksstotteRepository,
    val arenaVedtakRepository: ArenaVedtakRepository,
    val arenaVedtakService: ArenaVedtakService,
    val veilarboppfolgingClient: VeilarboppfolgingClient,
    val transactor: TransactionTemplate,
    val kafkaProducer: KafkaProducer
) {

    val log = LoggerFactory.getLogger(InnsatsbehovService::class.java)

    fun gjeldendeInnsatsbehov(fnr: Fnr): Innsatsbehov? {
        authService.sjekkTilgangTilFnr(fnr.get())
        val identer = brukerIdentService.hentIdenter(fnr)
        return gjeldendeInnsatsbehovMedKilder(identer).innsatsbehov
    }

    private data class InnsatsbehovMedKilder(
        val innsatsbehov: Innsatsbehov?,
        val fraArena: Boolean,
        val gjeldendeVedtak: Vedtak?,
        val arenaVedtak: List<ArenaVedtak>
    )

    private fun gjeldendeInnsatsbehovMedKilder(identer: BrukerIdenter): InnsatsbehovMedKilder {

        val vedtak: Vedtak? = vedtakRepository.hentGjeldendeVedtak(identer.aktorId.get())
        val arenaVedtakListe = arenaVedtakRepository.hentVedtakListe(identer.historiskeFnr.plus(identer.fnr))

        if (vedtak == null && arenaVedtakListe.isEmpty()) {
            return InnsatsbehovMedKilder(null, false, null, arenaVedtakListe)
        }

        val arenaVedtak = sisteArenaVedtakInnenforGjeldendeOppfolgingsperiode(arenaVedtakListe)

        if (
            (vedtak != null && arenaVedtak == null) ||
            (vedtak != null && arenaVedtak != null &&
                    vedtak.sistOppdatert.isAfter(arenaVedtak.fraDato))
        ) {
            return InnsatsbehovMedKilder(
                innsatsbehov = Innsatsbehov(
                    aktorId = identer.aktorId,
                    innsatsgruppe = vedtak.innsatsgruppe,
                    hovedmal = HovedmalMedOkeDeltakelse.fraHovedmal(vedtak.hovedmal)
                ),
                fraArena = false,
                vedtak,
                arenaVedtakListe
            )
        } else if (arenaVedtak != null) {
            return InnsatsbehovMedKilder(
                innsatsbehov = Innsatsbehov(
                    aktorId = identer.aktorId,
                    innsatsgruppe = ArenaInnsatsgruppe.tilInnsatsgruppe(arenaVedtak.innsatsgruppe),
                    hovedmal = HovedmalMedOkeDeltakelse.fraArenaHovedmal(arenaVedtak.hovedmal)
                ),
                fraArena = true,
                vedtak,
                arenaVedtakListe
            )
        }
        return InnsatsbehovMedKilder(null, false, vedtak, arenaVedtakListe)
    }

    private fun sisteArenaVedtakInnenforGjeldendeOppfolgingsperiode(arenaVedtakListe: List<ArenaVedtak>): ArenaVedtak? {
        val sisteArenaVedtak = finnSisteArenaVedtak(arenaVedtakListe)

        if (sisteArenaVedtak == null) {
            return null
        }

        val oppfolgingsperioder = veilarboppfolgingClient.hentOppfolgingsperioder(sisteArenaVedtak.fnr.get())
        val sisteOppfolgingsperiode: OppfolgingPeriodeDTO? =
            OppfolgingUtils.hentSisteOppfolgingsPeriode(oppfolgingsperioder).orElse(null)

        if (sisteOppfolgingsperiode != null &&
            OppfolgingUtils.erDatoInnenforOppfolgingsperiode(sisteArenaVedtak.fraDato, sisteOppfolgingsperiode)
        ) {
            return sisteArenaVedtak
        } else {
            return null
        }
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
        oppdaterInnsatsbehov(arenaVedtak.fnr)
    }

    fun oppdaterInnsatsbehov(fnr: Fnr) {
        val identer = brukerIdentService.hentIdenter(fnr)
        val gjeldendeVedtakDetailed = gjeldendeInnsatsbehovMedKilder(identer)

        transactor.executeWithoutResult {
            oppdaterGrunnlagForGjeldendeVedtak(gjeldendeVedtakDetailed)
        }

        kafkaProducer.sendInnsatsbehov(gjeldendeVedtakDetailed.innsatsbehov)
    }

    private fun oppdaterGrunnlagForGjeldendeVedtak(innsatsbehovMedKilder: InnsatsbehovMedKilder) {
        val (_, fraArena, vedtak, arenaVedtak) = innsatsbehovMedKilder

        if (fraArena) {
            if (vedtak != null && vedtak.isGjeldende) {
                vedtakRepository.settGjeldendeVedtakTilHistorisk(vedtak.aktorId)
            }
            // Håndterer endring av fnr for bruker
            if (arenaVedtak.size > 1) {
                val sisteVedtakFnr = finnSisteArenaVedtak(arenaVedtak)?.fnr
                arenaVedtakRepository.slettVedtak(arenaVedtak.map { it.fnr }.filterNot { it == sisteVedtakFnr })
            }
        } else {
            if (arenaVedtak.isNotEmpty()) {
                arenaVedtakRepository.slettVedtak(arenaVedtak.map { it.fnr })
            }
        }
    }

    fun behandleAvsluttOppfolging(melding: KafkaAvsluttOppfolging) {
        val identer = brukerIdentService.hentIdenter(AktorId(melding.aktorId))

        transactor.executeWithoutResult {
            vedtakRepository.settGjeldendeVedtakTilHistorisk(melding.aktorId)
            arenaVedtakService.slettArenaVedtakKopi(AktorId.of(melding.aktorId))
            val antall = arenaVedtakRepository.slettVedtak(identer.historiskeFnr.plus(identer.fnr))
            if (antall > 0) {
                log.info("Slettet $antall kopi(er) av vedtak fra Arena for aktorId=${identer.aktorId}")
            }
        }

        kafkaProducer.sendInnsatsbehov(null)
    }
}
