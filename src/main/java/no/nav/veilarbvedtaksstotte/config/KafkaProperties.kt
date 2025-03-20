package no.nav.veilarbvedtaksstotte.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.kafka")
data class KafkaProperties(
    val brokersUrl: String,
    val arenaVedtakTopic: String,
    val endringPaOppfolgingsBrukerTopic: String,
    val siste14aVedtakTopic: String,
    val sisteOppfolgingsperiodeTopic: String,
    val vedtakFattetDvhTopic: String,
    val vedtakSendtTopic: String,
    val vedtakStatusEndringTopic: String,
    val pdlAktorV2Topic: String,
    val gjeldende14aVedtakTopic: String
)
