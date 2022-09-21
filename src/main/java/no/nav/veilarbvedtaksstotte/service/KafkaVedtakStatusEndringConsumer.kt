package no.nav.veilarbvedtaksstotte.service

import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaVedtakStatusEndring
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Konsument for egen topic. Dette for å kunne gjøre asynkrone operasjoner som ikke interfererer med logikken som
 * fører til at meldinger på topic produseres. Ved å legge denne logikken i en konsument på egen topic, får man også
 * feilhåndtering på operasjon som skal utføres i etterkant.
 */
@Service
class KafkaVedtakStatusEndringConsumer(
    private val dvhRapporteringService: DvhRapporteringService,
) {

    private val log = LoggerFactory.getLogger(KafkaVedtakStatusEndringConsumer::class.java)

    fun konsumer(melding: ConsumerRecord<String, KafkaVedtakStatusEndring>) {
        log.info("Rapporterer til DVH asynkront")
        dvhRapporteringService.rapporterTilDvh(melding.value())
    }
}
