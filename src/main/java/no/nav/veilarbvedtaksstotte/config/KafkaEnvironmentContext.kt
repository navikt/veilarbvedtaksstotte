package no.nav.veilarbvedtaksstotte.config

import java.util.Properties

class KafkaEnvironmentContext(
    val onPremConsumerClientProperties: Properties,
    val onPremProducerClientProperties: Properties,
    val aivenConsumerClientProperties: Properties,
    val aivenProducerClientProperties: Properties
)
