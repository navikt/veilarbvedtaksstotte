package no.nav.veilarbvedtaksstotte.service

import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.OppfolgingPeriodeDTO
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.VeilarboppfolgingClient
import no.nav.veilarbvedtaksstotte.domain.BrukerIdenter
import no.nav.veilarbvedtaksstotte.domain.vedtak.ArenaVedtak
import no.nav.veilarbvedtaksstotte.domain.vedtak.ArenaVedtak.ArenaInnsatsgruppe
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsbehov
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsbehov.HovedmalMedOkeDeltakelse
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak
import no.nav.veilarbvedtaksstotte.repository.ArenaVedtakRepository
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository
import no.nav.veilarbvedtaksstotte.utils.OppfolgingUtils
import no.nav.veilarbvedtaksstotte.utils.TimeUtils.toLocalDate
import no.nav.veilarbvedtaksstotte.utils.TimeUtils.toZonedDateTime
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.time.*

@Service
class InnsatsbehovService(
    val transactor: TransactionTemplate,
    val kafkaProducerService: KafkaProducerService,
    val vedtakRepository: VedtaksstotteRepository,
    val arenaVedtakRepository: ArenaVedtakRepository,
    val authService: AuthService,
    val brukerIdentService: BrukerIdentService,
    val arenaVedtakService: ArenaVedtakService,
    val veilarboppfolgingClient: VeilarboppfolgingClient,
) {

    val log = LoggerFactory.getLogger(InnsatsbehovService::class.java)

    fun gjeldendeInnsatsbehov(fnr: Fnr): Innsatsbehov? {
        authService.sjekkTilgangTilBruker(fnr)
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

        val gjeldendeVedtak: Vedtak? = vedtakRepository.hentGjeldendeVedtak(identer.aktorId.get())
        val arenaVedtakListe = arenaVedtakRepository.hentVedtakListe(identer.historiskeFnr.plus(identer.fnr))

        if (gjeldendeVedtak == null && arenaVedtakListe.isEmpty()) {
            return InnsatsbehovMedGrunnlag(
                innsatsbehov = null,
                fraArena = false,
                gjeldendeVedtak = null,
                arenaVedtak = arenaVedtakListe
            )
        }

        val arenaVedtak = sisteArenaVedtakInnenforGjeldendeOppfolgingsperiode(arenaVedtakListe)

        // Gjeldende vedtak fra denne løsningen
        if (
            (gjeldendeVedtak != null && arenaVedtak == null) ||
            (gjeldendeVedtak != null && arenaVedtak != null &&
                    gjeldendeVedtak.vedtakFattet.isAfter(arenaVedtak.beregnetFattetTidspunkt()))
        ) {
            return InnsatsbehovMedGrunnlag(
                innsatsbehov = Innsatsbehov(
                    aktorId = identer.aktorId,
                    innsatsgruppe = gjeldendeVedtak.innsatsgruppe,
                    hovedmal = HovedmalMedOkeDeltakelse.fraHovedmal(gjeldendeVedtak.hovedmal)
                ),
                fraArena = false,
                gjeldendeVedtak = gjeldendeVedtak,
                arenaVedtak = arenaVedtakListe
            )

            // Gjeldende vedtak fra Arena
        } else if (arenaVedtak != null) {
            return InnsatsbehovMedGrunnlag(
                innsatsbehov = Innsatsbehov(
                    aktorId = identer.aktorId,
                    innsatsgruppe = ArenaInnsatsgruppe.tilInnsatsgruppe(arenaVedtak.innsatsgruppe),
                    hovedmal = HovedmalMedOkeDeltakelse.fraArenaHovedmal(arenaVedtak.hovedmal)
                ),
                fraArena = true,
                gjeldendeVedtak = gjeldendeVedtak,
                arenaVedtak = arenaVedtakListe
            )
        }

        // Ingen gjeldende vedtak
        return InnsatsbehovMedGrunnlag(
            innsatsbehov = null,
            fraArena = false,
            gjeldendeVedtak = gjeldendeVedtak,
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
            innenforOppfolgingsperiode(sisteArenaVedtak, sisteOppfolgingsperiode)
        ) {
            return sisteArenaVedtak
        } else {
            return null
        }
    }

    private fun innenforOppfolgingsperiode(
        arenaVedtak: ArenaVedtak,
        oppfolgingsperiode: OppfolgingPeriodeDTO
    ): Boolean {

        // Oppfolgingsperiode justert til å gjelde fra midnatt siden vi ikke kan beregne tidspunkt for når vedtaket ble
        // fattet dersom ikke Kafka-melding sendes samme dag som vedtaket fattes og får operation timestamp som kan brukes.
        val oppfolgingsperiodeFraMidnatt =
            OppfolgingPeriodeDTO(
                ZonedDateTime.of(toLocalDate(oppfolgingsperiode.startDato), LocalTime.MIDNIGHT, ZoneId.systemDefault()),
                oppfolgingsperiode.sluttDato
            )

        return OppfolgingUtils
            .erDatoInnenforOppfolgingsperiode(
                toZonedDateTime(LocalDateTime.of(arenaVedtak.fraDato, LocalTime.MIDNIGHT)), oppfolgingsperiodeFraMidnatt
            ) || skalUnntakshandteresSomInnenforOppfolgingsperiode(arenaVedtak, oppfolgingsperiodeFraMidnatt)
    }

    // Unntakshåndtering for vedtak som er fattet i innen 2017 og som er eldre enn oppfølgingsperiodens startdato som
    // også må være i 2017.
    private fun skalUnntakshandteresSomInnenforOppfolgingsperiode(
        arenaVedtak: ArenaVedtak,
        oppfolgingsperiode: OppfolgingPeriodeDTO
    ): Boolean {
        return arenaVedtak.fraDato.year <= 2017 &&
                oppfolgingsperiode.startDato.year == 2017 &&
                toLocalDate(oppfolgingsperiode.startDato).isAfter(arenaVedtak.fraDato)
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
        oppdaterInnsatsbehov(arenaVedtak)
    }

    private fun oppdaterInnsatsbehov(arenaVedtak: ArenaVedtak) {
        val identer = brukerIdentService.hentIdenter(arenaVedtak.fnr)
        val (innsatsbehov, fraArena, gjeldendeVedtak, arenaVedtakListe) = gjeldendeInnsatsbehovMedKilder(identer)

        if (fraArena && gjeldendeVedtak != null) {
            vedtakRepository.settGjeldendeVedtakTilHistorisk(gjeldendeVedtak.aktorId)
        }

        if (fraArena &&
            // hindrer at vi republiserer gjeldende innsatsbehov dersom eldre meldinger skulle bli konsumert:
            finnSisteArenaVedtak(arenaVedtakListe) == arenaVedtak
        ) {
            kafkaProducerService.sendInnsatsbehov(innsatsbehov)
        }
    }
}
