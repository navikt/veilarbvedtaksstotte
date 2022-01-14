package no.nav.veilarbvedtaksstotte.metrics

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.MeterBinder
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime

@Component
class DokumentdistribusjonMeterBinder(
    val vedtaksstotteRepository: VedtaksstotteRepository
) : MeterBinder {

    override fun bindTo(registry: MeterRegistry) {
        // Denne metrikken er til bruk for alarm på dokumentintegrasjon (dokarkiv og dokdistfordeling) dersom brev har
        // blitt journalført men ikke distribuert. Ved gammel integrasjon har vi ikke info om distribusjon, slik
        // at denne metrikken kan bare brukes til alarm der vedtak er fattet ved bruk av ny dokumentintegrasjon.
        val fra = LocalDate.now().minusDays(10).atStartOfDay()
        Gauge.builder("antall_journalforte_vedtak_uten_dokumentbestilling") {
            antallJournalforteVedtakUtenDokumentbestilling(fra)
        }.description("Antall journalførte vedtak uten dokumentbestilling fra $fra til nå.").register(registry)
    }

    fun antallJournalforteVedtakUtenDokumentbestilling(fra: LocalDateTime): Int {
        return vedtaksstotteRepository.hentAntallJournalforteVedtakUtenDokumentbestilling(
            fra,
            // Trekker fra et minutt for a unngå å telle med vedtak som holder på å bli distribuert
            LocalDateTime.now().minusMinutes(1)
        )
    }
}
