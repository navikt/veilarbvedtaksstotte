package no.nav.veilarbvedtaksstotte.service

import lombok.extern.slf4j.Slf4j
import no.nav.common.types.identer.AktorId
import no.nav.person.pdl.aktor.v2.Aktor
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.math.log

@Service
@Slf4j
class BrukerIdenterService {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun behandlePdlAktorV2Melding(aktorRecord: ConsumerRecord<AktorId, Aktor>) {
        logger.info("Konsumerte melding på pdl.aktor-v2 topic")

        // 2025-02-28: TODO Implementere referanseimplementasjonen til PDL
        // Sjå https://pdl-docs.ansatt.nav.no/ekstern/index.html#identitetshendelser_pa_kafka
        //
        // "Som konsument av denne topicen for bygging av egen cache anbefales følgende fremgangsmåte: Når en melding
        // mottas, henter man ut alle identer fra meldingen. For hver av disse identene, finner man alle tilknyttede
        // personer i egen cache. For hver av disse personene, sletter man alle identer. Deretter lagrer man identene i
        // den mottatte meldingen som én person."
    }
}