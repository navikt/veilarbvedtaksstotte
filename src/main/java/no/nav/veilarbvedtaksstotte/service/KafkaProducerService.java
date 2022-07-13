package no.nav.veilarbvedtaksstotte.service;

import io.confluent.kafka.serializers.KafkaAvroSerializer;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.kafka.producer.feilhandtering.KafkaProducerRecordStorage;
import no.nav.pto_schema.kafka.avro.Vedtak14aFattetDvh;
import no.nav.pto_schema.kafka.avro.Vedtak14aFattetDvhHovedmalKode;
import no.nav.pto_schema.kafka.avro.Vedtak14aFattetDvhInnsatsgruppeKode;
import no.nav.veilarbvedtaksstotte.config.KafkaConfig;
import no.nav.veilarbvedtaksstotte.config.KafkaProperties;
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaVedtakSendt;
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaVedtakStatusEndring;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Hovedmal;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsgruppe;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Siste14aVedtak;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.stereotype.Service;

import static no.nav.common.kafka.producer.util.ProducerUtils.serializeJsonRecord;
import static no.nav.common.kafka.producer.util.ProducerUtils.serializeRecord;
import static no.nav.veilarbvedtaksstotte.utils.TimeUtils.toInstant;

@Slf4j
@Service
public class KafkaProducerService {
    private final KafkaProducerRecordStorage producerRecordStorage;
    private final KafkaProperties kafkaProperties;
    private final KafkaAvroSerializer kafkaAvroSerializer;

    public KafkaProducerService(KafkaProducerRecordStorage producerRecordStorage,
                                KafkaProperties kafkaProperties,
                                KafkaConfig.KafkaAvroContext kafkaAvroContext) {
        this.producerRecordStorage = producerRecordStorage;
        this.kafkaProperties = kafkaProperties;
        this.kafkaAvroSerializer = new KafkaAvroSerializer(null, kafkaAvroContext.getConfig());
    }

    public void sendVedtakStatusEndring(KafkaVedtakStatusEndring vedtakStatusEndring) {
        ProducerRecord<byte[], byte[]> producerRecord =
                serializeJsonRecord(
                        new ProducerRecord<>(
                                kafkaProperties.getVedtakStatusEndringTopic(),
                                vedtakStatusEndring.getAktorId(),
                                vedtakStatusEndring));

        producerRecordStorage.store(producerRecord);
    }

    public void sendVedtakSendt(KafkaVedtakSendt vedtakSendt) {
        ProducerRecord<byte[], byte[]> producerRecord =
                serializeJsonRecord(
                        new ProducerRecord<>(
                                kafkaProperties.getVedtakSendtTopic(),
                                vedtakSendt.getAktorId(),
                                vedtakSendt));

        producerRecordStorage.store(producerRecord);
    }

    public void sendSiste14aVedtak(Siste14aVedtak siste14aVedtak) {
        ProducerRecord<byte[], byte[]> producerRecord =
                serializeJsonRecord(
                        new ProducerRecord<>(
                                kafkaProperties.getSiste14aVedtakTopic(),
                                siste14aVedtak.getAktorId().get(),
                                siste14aVedtak));

        producerRecordStorage.store(producerRecord);
    }

    public void sendVedtakFattetDvh(Vedtak vedtak) {
        Vedtak14aFattetDvh vedtak14aFattetDvh = mapVedtakTilDvh(vedtak);
        ProducerRecord<byte[], byte[]> producerRecord = null;
        try {
            producerRecord = serializeAvroRecord(
                    new ProducerRecord<>(
                            kafkaProperties.getVedtakFattetDvhTopic(),
                            vedtak14aFattetDvh.getAktorId().toString(),
                            vedtak14aFattetDvh
                    )
            );
        } catch (Exception e) {
            log.error("Klarte ikke serialisere Avro-melding", e);
        }

        if (producerRecord != null) {
            producerRecordStorage.store(producerRecord);
        }
    }

    private  ProducerRecord<byte[], byte[]> serializeAvroRecord(ProducerRecord<String, Object> producerRecord) {
        return serializeRecord(producerRecord, new StringSerializer(), kafkaAvroSerializer);
    }

    private static Vedtak14aFattetDvh mapVedtakTilDvh(Vedtak vedtak) {
        return Vedtak14aFattetDvh.newBuilder()
                .setId(vedtak.getId())
                .setAktorId(vedtak.getAktorId())
                .setHovedmalKode(mapHovedmalTilAvroType(vedtak.getHovedmal()))
                .setInnsatsgruppeKode(mapInnsatsgruppeTilAvroType(vedtak.getInnsatsgruppe()))
                .setVedtakFattet(toInstant(vedtak.getVedtakFattet()))
                .setOppfolgingsenhetId(vedtak.getOppfolgingsenhetId())
                .setVeilederIdent(vedtak.getVeilederIdent())
                .setBeslutterIdent(vedtak.getBeslutterIdent())
                .build();
    }

    private static Vedtak14aFattetDvhHovedmalKode mapHovedmalTilAvroType(Hovedmal hovedmal) {
        if (hovedmal == null) {
            return null;
        }

        switch (hovedmal) {
            case SKAFFE_ARBEID:
                return Vedtak14aFattetDvhHovedmalKode.SKAFFE_ARBEID;
            case BEHOLDE_ARBEID:
                return Vedtak14aFattetDvhHovedmalKode.BEHOLDE_ARBEID;
            default:
                throw new IllegalStateException("Manglende mapping av hovedmål");
        }
    }

    private static Vedtak14aFattetDvhInnsatsgruppeKode mapInnsatsgruppeTilAvroType(Innsatsgruppe innsatsgruppe) {
        if (innsatsgruppe == null) {
            return null;
        }

        switch (innsatsgruppe) {
            case STANDARD_INNSATS:
                return Vedtak14aFattetDvhInnsatsgruppeKode.STANDARD_INNSATS;
            case SITUASJONSBESTEMT_INNSATS:
                return Vedtak14aFattetDvhInnsatsgruppeKode.SITUASJONSBESTEMT_INNSATS;
            case SPESIELT_TILPASSET_INNSATS:
                return Vedtak14aFattetDvhInnsatsgruppeKode.SPESIELT_TILPASSET_INNSATS;
            case GRADERT_VARIG_TILPASSET_INNSATS:
                return Vedtak14aFattetDvhInnsatsgruppeKode.GRADERT_VARIG_TILPASSET_INNSATS;
            case VARIG_TILPASSET_INNSATS:
                return Vedtak14aFattetDvhInnsatsgruppeKode.VARIG_TILPASSET_INNSATS;
            default:
                throw new IllegalStateException("Manglende mapping av innsatsgruppe");
        }
    }
}
