package no.nav.veilarbvedtaksstotte.metrics

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.MeterBinder
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class DokumentdistribusjonMeterBinder(
    val vedtaksstotteRepository: VedtaksstotteRepository
) : MeterBinder {

    override fun bindTo(registry: MeterRegistry) {
        Gauge.builder("antall_fattet_vedtak_uten_journalforing") {
            antallFattetVedtakUtenJournalforing()
        }.description("Antall fattet vedtak som er ikke journalført").register(registry)

        Gauge.builder("antall_journalforte_vedtak_uten_dokumentbestilling") {
            antallJournalforteVedtakUtenDokumentbestilling()
        }.description("Antall journalførte vedtak uten dokumentbestilling.").register(registry)

        Gauge.builder("antall_vedtak_med_feilende_dokumentbestilling") {
            antallJournalforteVedtakMedFeiletDokumentbestilling()
        }.description("Antall vedtak med feilende dokumentbestilling. Disse må rettes manuelt.").register(registry)
    }

    fun antallJournalforteVedtakUtenDokumentbestilling(): Int {
        return vedtaksstotteRepository.hentAntallJournalforteVedtakUtenDokumentbestilling(
            // Trekker fra tid distribusjon schedule kjører (hvert 10. min) + 3 min unngå å telle med vedtak som holder
            // på å bli distribuert
            LocalDateTime.now().minusMinutes(13)
        )
    }

    fun antallFattetVedtakUtenJournalforing(): Int {
        return vedtaksstotteRepository.hentFattetVedtakUtenJournalforing(LocalDateTime.now().minusMinutes(5))
    }

    fun antallJournalforteVedtakMedFeiletDokumentbestilling(): Int {
        return vedtaksstotteRepository.hentAntallVedtakMedFeilendeDokumentbestilling()
    }
}
