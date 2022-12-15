package no.nav.veilarbvedtaksstotte.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "app.kafka")
data class KafkaProperties(
    val brokersUrl: String,
    val endringPaAvsluttOppfolgingOnpremTopic: String,
    val endringPaAvsluttOppfolgingTopic: String,
    val endringPaOppfolgingsBrukerTopic: String,
    val vedtakSendtTopic: String,
    val vedtakStatusEndringTopic: String,
    val arenaVedtakTopic: String,
    val siste14aVedtakTopic: String,
    val vedtakFattetDvhTopic: String
)
