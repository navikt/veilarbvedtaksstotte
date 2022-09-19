package no.nav.veilarbvedtaksstotte.kafka

import org.apache.kafka.clients.producer.KafkaProducer

class KafkaTestProducer(val config: Map<String, Any>) : KafkaProducer<String, String>(config)
