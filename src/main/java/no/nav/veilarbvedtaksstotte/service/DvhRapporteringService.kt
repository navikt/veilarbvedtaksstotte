package no.nav.veilarbvedtaksstotte.service

import io.confluent.kafka.serializers.KafkaAvroSerializer
import no.nav.common.kafka.producer.feilhandtering.KafkaProducerRecordStorage
import no.nav.common.kafka.producer.util.ProducerUtils.serializeRecord
import no.nav.pto_schema.kafka.avro.Vedtak14aFattetDvh
import no.nav.pto_schema.kafka.avro.Vedtak14aFattetDvhHovedmalKode
import no.nav.pto_schema.kafka.avro.Vedtak14aFattetDvhInnsatsgruppeKode
import no.nav.veilarbvedtaksstotte.config.KafkaConfig
import no.nav.veilarbvedtaksstotte.config.KafkaProperties
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaVedtakStatusEndring
import no.nav.veilarbvedtaksstotte.domain.kafka.VedtakStatusEndring.VEDTAK_SENDT
import no.nav.veilarbvedtaksstotte.domain.vedtak.Hovedmal
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsgruppe
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository
import no.nav.veilarbvedtaksstotte.utils.TimeUtils.toInstant
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

/**
 * Rapportering til DVH på Kafka-topic gjøres asynkront for å få feilhåndtering. Dette fordi topic bruker Avro, og må
 * kontakte schema-registry gjennom http-kall ved serialisering av meldinger.
 */
@Service
class DvhRapporteringService(
    private val vedtaksstotteRepository: VedtaksstotteRepository,
    @Lazy private val kafkaProducerRecordStorage: KafkaProducerRecordStorage,
    private val kafkaProperties: KafkaProperties,
    kafkaAvroContext: KafkaConfig.KafkaAvroContext
) {

    private val kafkaAvroSerializer: KafkaAvroSerializer = KafkaAvroSerializer(null, kafkaAvroContext.config)


    fun rapporterTilDvh(statusEndring: KafkaVedtakStatusEndring) {
        if (statusEndring.vedtakStatusEndring == VEDTAK_SENDT) {
            val vedtak = vedtaksstotteRepository.hentVedtak(statusEndring.vedtakId)

            produserVedtakFattetDvhMelding(vedtak)
        }
    }

    fun produserVedtakFattetDvhMelding(vedtak: Vedtak) {
        val vedtak14aFattetDvh = mapVedtakTilDvh(vedtak)

        val producerRecord = serializeAvroRecord(
            ProducerRecord(
                kafkaProperties.vedtakFattetDvhTopic, vedtak14aFattetDvh.aktorId.toString(), vedtak14aFattetDvh
            )
        )

        kafkaProducerRecordStorage.store(producerRecord);
    }

    private fun mapVedtakTilDvh(vedtak: Vedtak): Vedtak14aFattetDvh {
        return Vedtak14aFattetDvh.newBuilder()
            .setId(vedtak.id)
            .setAktorId(vedtak.aktorId)
            .setHovedmalKode(mapHovedmalTilAvroType(vedtak.hovedmal))
            .setInnsatsgruppeKode(mapInnsatsgruppeTilAvroType(vedtak.innsatsgruppe))
            .setVedtakFattet(toInstant(vedtak.vedtakFattet))
            .setOppfolgingsenhetId(vedtak.oppfolgingsenhetId)
            .setVeilederIdent(vedtak.veilederIdent)
            .setBeslutterIdent(vedtak.beslutterIdent).build()
    }

    private fun mapHovedmalTilAvroType(hovedmal: Hovedmal?): Vedtak14aFattetDvhHovedmalKode? {
        return when (hovedmal) {
            Hovedmal.SKAFFE_ARBEID -> Vedtak14aFattetDvhHovedmalKode.SKAFFE_ARBEID;
            Hovedmal.BEHOLDE_ARBEID -> Vedtak14aFattetDvhHovedmalKode.BEHOLDE_ARBEID;
            null -> null
        }
    }

    private fun mapInnsatsgruppeTilAvroType(innsatsgruppe: Innsatsgruppe?): Vedtak14aFattetDvhInnsatsgruppeKode? {
        return when (innsatsgruppe) {
            Innsatsgruppe.STANDARD_INNSATS -> Vedtak14aFattetDvhInnsatsgruppeKode.STANDARD_INNSATS;
            Innsatsgruppe.SITUASJONSBESTEMT_INNSATS -> Vedtak14aFattetDvhInnsatsgruppeKode.SITUASJONSBESTEMT_INNSATS;
            Innsatsgruppe.SPESIELT_TILPASSET_INNSATS -> Vedtak14aFattetDvhInnsatsgruppeKode.SPESIELT_TILPASSET_INNSATS;
            Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS -> Vedtak14aFattetDvhInnsatsgruppeKode.GRADERT_VARIG_TILPASSET_INNSATS;
            Innsatsgruppe.VARIG_TILPASSET_INNSATS -> Vedtak14aFattetDvhInnsatsgruppeKode.VARIG_TILPASSET_INNSATS;
            null -> null
        }
    }

    fun serializeAvroRecord(producerRecord: ProducerRecord<String, Any>): ProducerRecord<ByteArray, ByteArray> {
        return serializeRecord(producerRecord, StringSerializer(), kafkaAvroSerializer);
    }
}
