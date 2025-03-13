package no.nav.veilarbvedtaksstotte.domain.kafka

import no.nav.veilarbvedtaksstotte.domain.vedtak.Hovedmal
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsgruppe
import java.time.LocalDateTime

class KafkaVedtakSendt(
    val id: Long,
    val vedtakSendt: LocalDateTime,
    val innsatsgruppe: Innsatsgruppe,
    val hovedmal: Hovedmal,
    val aktorId: String,
    val enhetId: String
)
