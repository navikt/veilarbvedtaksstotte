package no.nav.veilarbvedtaksstotte.domain.kafka

import no.nav.veilarbvedtaksstotte.domain.vedtak.Hovedmal
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsgruppe
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak
import java.time.LocalDateTime

data class KafkaVedtakSendt(
    val id: Long,
    val vedtakSendt: LocalDateTime,
    val innsatsgruppe: Innsatsgruppe,
    val hovedmal: Hovedmal,
    val aktorId: String,
    val enhetId: String
)

fun Vedtak.toKafkaVedtakSendt(): KafkaVedtakSendt {
    return KafkaVedtakSendt(
        id = id,
        vedtakSendt = vedtakFattet,
        innsatsgruppe = innsatsgruppe,
        hovedmal = hovedmal,
        aktorId = aktorId,
        enhetId = oppfolgingsenhetId
    )
}