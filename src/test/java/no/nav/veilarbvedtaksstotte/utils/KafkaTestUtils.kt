package no.nav.veilarbvedtaksstotte.utils

import no.nav.common.kafka.consumer.ConsumeStatus
import no.nav.common.kafka.consumer.KafkaConsumerClient
import no.nav.common.kafka.consumer.TopicConsumer
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder
import no.nav.common.kafka.consumer.util.deserializer.Deserializers
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.Deserializer
import java.util.*

object KafkaTestUtils {

    fun kafkaTestConsumerProperties(brokerUrl: String): Properties {
        val props = Properties()
        props[CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG] = brokerUrl
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = ByteArrayDeserializer::class.java
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = ByteArrayDeserializer::class.java
        props[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = false
        props[ConsumerConfig.GROUP_ID_CONFIG] = "test-consumer"
        props[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = 5 * 60 * 1000
        props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        return props
    }

    inline fun <reified Message> testConsumer(
        brokerUrl: String,
        topicName: String,
        valueDeserializer: Deserializer<Message> = Deserializers.jsonDeserializer(Message::class.java),
        crossinline onMessage: (ConsumerRecord<String, Message>) -> Unit
    ): KafkaConsumerClient {
        return KafkaConsumerClientBuilder.builder()
            .withProperties(kafkaTestConsumerProperties(brokerUrl))
            .withTopicConfig(
                KafkaConsumerClientBuilder.TopicConfig<String, Message>()
                    .withConsumerConfig(
                        topicName,
                        Deserializers.stringDeserializer(),
                        valueDeserializer,
                        TopicConsumer { record ->
                            onMessage(record)
                            ConsumeStatus.OK
                        }
                    )
            )
            .build()
    }
}
