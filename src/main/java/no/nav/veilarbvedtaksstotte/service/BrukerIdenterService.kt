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

        /*
            1.
         */
    }
}