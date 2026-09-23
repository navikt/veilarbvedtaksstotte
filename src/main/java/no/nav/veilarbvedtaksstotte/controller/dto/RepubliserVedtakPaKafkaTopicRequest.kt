package no.nav.veilarbvedtaksstotte.controller.dto

data class RepubliserVedtakPaKafkaTopicRequest(
    val vedtaksIDer: List<String>,
    val kafkaTopic: String
)

data class RepubliserVedtakPaBigQueryRequest(
    val vedtaksIDer: List<String>
)

data class RepubliserSakStatistikkRadPaBigQueryRequest(
    val sekvensnummer: Long
)
