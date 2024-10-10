package no.nav.veilarbvedtaksstotte.metrics

import no.nav.veilarbvedtaksstotte.repository.KafkaConsumerRecordRepository
import no.nav.veilarbvedtaksstotte.utils.DatabaseTest
import no.nav.veilarbvedtaksstotte.utils.DbTestUtils.cleanupDb
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.io.encoding.Base64.Default.decode

class KafkaConsumerMeterBinderTest : DatabaseTest() {

        lateinit var kafkaConsumerRecordRepository: KafkaConsumerRecordRepository
        lateinit var kafkaConsumerMeterBinder: KafkaConsumerMeterBinder

        @BeforeEach
        fun setup() {
            kafkaConsumerRecordRepository = KafkaConsumerRecordRepository(jdbcTemplate, transactor)
            kafkaConsumerMeterBinder = KafkaConsumerMeterBinder(kafkaConsumerRecordRepository)
            cleanupDb(jdbcTemplate)
        }

    @Test
    fun `finner antall rader i tabell KAFKACONSUMERRECORD MED RETRIES OVER 4`() {
        val key = "16056212345"
        val byteArray: ByteArray = key.toByteArray(Charsets.UTF_8)
        println(byteArray)
        jdbcTemplate.update(
            """
            INSERT INTO kafka_consumer_record(
                ID, TOPIC, PARTITION, RECORD_OFFSET, RETRIES, LAST_RETRY, KEY, VALUE, HEADERS_JSON, RECORD_TIMESTAMP, CREATED_AT
            ) 
            VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
            1,
            "topic",
            1,
            1,
            5,
            LocalDateTime.now(),
            16056212345,
            "value",
            "{}",
            LocalDateTime.now(),
            LocalDateTime.now(),
        )

        val antallRader =
            kafkaConsumerMeterBinder.antallRaderIKafkaConsumerRecordOverRetriesgrense()

        assertEquals(1, antallRader)
    }


}
