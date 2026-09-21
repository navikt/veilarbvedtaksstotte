package no.nav.veilarbvedtaksstotte.controller.dto

data class RepubliserVedtakPaKafkaTopicRequest(
    val vedtaksIDer: List<String>,
    val kafkaTopic: String
)
