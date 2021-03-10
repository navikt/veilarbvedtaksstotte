package no.nav.veilarbvedtaksstotte.service

import no.nav.common.types.identer.EksternBrukerId
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
    val transactor: TransactionTemplate,
    val kafkaProducer: KafkaProducer,
    val vedtakRepository: VedtaksstotteRepository,
    val arenaVedtakRepository: ArenaVedtakRepository,
    val authService: AuthService,
    val brukerIdentService: BrukerIdentService,
    val arenaVedtakService: ArenaVedtakService,
    val veilarboppfolgingClient: VeilarboppfolgingClient,
) {

    val log = LoggerFactory.getLogger(InnsatsbehovService::class.java)

    fun gjeldendeInnsatsbehov(fnr: Fnr): Innsatsbehov? {
        authService.sjekkTilgangTilFnr(fnr.get())
        val identer: BrukerIdenter = brukerIdentService.hentIdenter(fnr)
        return gjeldendeInnsatsbehovMedKilder(identer).innsatsbehov
    }

    private data class InnsatsbehovMedGrunnlag(
        val innsatsbehov: Innsatsbehov?,
        val fraArena: Boolean,
        val gjeldendeVedtak: Vedtak?,
        val arenaVedtak: List<ArenaVedtak>
    )

    private fun gjeldendeInnsatsbehovMedKilder(identer: BrukerIdenter): InnsatsbehovMedGrunnlag {

        val vedtak: Vedtak? = vedtakRepository.hentGjeldendeVedtak(identer.aktorId.get())
        val arenaVedtakListe = arenaVedtakRepository.hentVedtakListe(identer.historiskeFnr.plus(identer.fnr))

        if (vedtak == null && arenaVedtakListe.isEmpty()) {
            return InnsatsbehovMedGrunnlag(
                innsatsbehov = null,
                fraArena = false,
                gjeldendeVedtak = null,
                arenaVedtak = arenaVedtakListe
            )
        }

        val arenaVedtak = sisteArenaVedtakInnenforGjeldendeOppfolgingsperiode(arenaVedtakListe)

        if (
            (vedtak != null && arenaVedtak == null) ||
            (vedtak != null && arenaVedtak != null &&
                    vedtak.vedtakFattet.isAfter(arenaVedtak.fraDato))
        ) {
            return InnsatsbehovMedGrunnlag(
                innsatsbehov = Innsatsbehov(
                    aktorId = identer.aktorId,
                    innsatsgruppe = vedtak.innsatsgruppe,
                    hovedmal = HovedmalMedOkeDeltakelse.fraHovedmal(vedtak.hovedmal)
                ),
                fraArena = false,
                gjeldendeVedtak = vedtak,
                arenaVedtak = arenaVedtakListe
            )
        } else if (arenaVedtak != null) {
            return InnsatsbehovMedGrunnlag(
                innsatsbehov = Innsatsbehov(
                    aktorId = identer.aktorId,
                    innsatsgruppe = ArenaInnsatsgruppe.tilInnsatsgruppe(arenaVedtak.innsatsgruppe),
                    hovedmal = HovedmalMedOkeDeltakelse.fraArenaHovedmal(arenaVedtak.hovedmal)
                ),
                fraArena = true,
                gjeldendeVedtak = vedtak,
                arenaVedtak = arenaVedtakListe
            )
        }
        return InnsatsbehovMedGrunnlag(
            innsatsbehov = null,
            fraArena = false,
            gjeldendeVedtak = vedtak,
            arenaVedtak = arenaVedtakListe
        )
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
            OppfolgingUtils.erOppfolgingsperiodeAktiv(sisteOppfolgingsperiode) &&
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

    fun oppdaterInnsatsbehov(brukerId: EksternBrukerId) {
        val identer = brukerIdentService.hentIdenter(brukerId)
        val gjeldendeVedtakDetailed = gjeldendeInnsatsbehovMedKilder(identer)

        transactor.executeWithoutResult {
            oppdaterGrunnlagForGjeldendeVedtak(gjeldendeVedtakDetailed)
        }

        kafkaProducer.sendInnsatsbehov(gjeldendeVedtakDetailed.innsatsbehov)
    }

    private fun oppdaterGrunnlagForGjeldendeVedtak(innsatsbehovMedGrunnlag: InnsatsbehovMedGrunnlag) {
        val (innsatsbehov, fraArena, vedtak, arenaVedtakListe) = innsatsbehovMedGrunnlag

        if (fraArena) {
            settVedtakTilHistorisk(vedtak)
            slettGamleArenaVedtak(arenaVedtakListe)
        } else if (innsatsbehov != null && arenaVedtakListe.isNotEmpty()) {
            // Vedtak fra denne løsningen
            arenaVedtakRepository.slettVedtak(arenaVedtakListe.map { it.fnr })
        } else {
            // Ingen gjeldende vedtak
            slettGamleArenaVedtak(arenaVedtakListe)
        }
    }

    private fun settVedtakTilHistorisk(vedtak: Vedtak?) {
        if (vedtak != null && vedtak.isGjeldende) {
            vedtakRepository.settGjeldendeVedtakTilHistorisk(vedtak.aktorId)
        }
    }

    private fun slettGamleArenaVedtak(arenaVedtakListe: List<ArenaVedtak>) {
        // Håndterer endring av fnr for bruker
        if (arenaVedtakListe.size > 1) {
            val sisteVedtakFnr = finnSisteArenaVedtak(arenaVedtakListe)?.fnr
            arenaVedtakRepository.slettVedtak(arenaVedtakListe.map { it.fnr }.filterNot { it == sisteVedtakFnr })
        }
    }

    fun behandleAvsluttOppfolging(melding: KafkaAvsluttOppfolging) {
        transactor.executeWithoutResult {
            vedtakRepository.settGjeldendeVedtakTilHistorisk(melding.aktorId)
        }
        kafkaProducer.sendInnsatsbehov(null)
    }
}
