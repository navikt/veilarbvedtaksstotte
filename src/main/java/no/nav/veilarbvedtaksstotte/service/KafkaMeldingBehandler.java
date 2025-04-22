package no.nav.veilarbvedtaksstotte.service;

import org.apache.kafka.clients.consumer.ConsumerRecord;

public interface KafkaMeldingBehandler<K, V> {
    void behandleMelding(ConsumerRecord<K, V> melding);
}
