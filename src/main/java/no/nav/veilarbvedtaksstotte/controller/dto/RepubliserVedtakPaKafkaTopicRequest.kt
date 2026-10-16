package no.nav.veilarbvedtaksstotte.controller.dto

data class RepubliserVedtakPaKafkaTopicRequest(
    val vedtaksIDer: List<String>,
    val kafkaTopic: String
)

data class RepubliserVedtakPaBigQueryRequest(
    val vedtaksIDer: List<String>
)

data class RepubliserSakStatistikkRadPaBigQueryRequest(
    val sekvensnumre: List<Long>
)

data class PubliserSakStatistikkRadPaBigQueryRequest(
    val sekvensnumre: List<Long>
)
