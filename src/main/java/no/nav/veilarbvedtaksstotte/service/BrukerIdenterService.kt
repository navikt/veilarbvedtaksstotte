package no.nav.veilarbvedtaksstotte.service

import lombok.extern.slf4j.Slf4j
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Id
import no.nav.person.pdl.aktor.v2.Aktor
import no.nav.person.pdl.aktor.v2.Identifikator
import no.nav.veilarbvedtaksstotte.repository.BrukerIdenterRepository
import no.nav.veilarbvedtaksstotte.repository.Gruppe
import no.nav.veilarbvedtaksstotte.repository.Ident
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Slf4j
class BrukerIdenterService(
    @Autowired val brukerIdenterRepository: BrukerIdenterRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Behandlingen er basert på referanseimplementasjonen til PDL:
     *
     * > "Som konsument av denne topicen for bygging av egen cache anbefales følgende fremgangsmåte: Når en melding
     * mottas, henter man ut alle identer fra meldingen. For hver av disse identene, finner man alle tilknyttede
     * personer i egen cache. For hver av disse personene, sletter man alle identer. Deretter lagrer man identene i
     * den mottatte meldingen som én person."
     *
     * @see <a href="https://pdl-docs.ansatt.nav.no/ekstern/index.html#identitetshendelser_pa_kafka">PDL - Identitetshendelser på Kafka</a>
     */
    @Transactional
    fun behandlePdlAktorV2Melding(aktorRecord: ConsumerRecord<AktorId, Aktor>) {
        logger.info("Behandler melding: topic ${aktorRecord.topic()}, offset ${aktorRecord.offset()}, partisjon ${aktorRecord.partition()}")

        val identer = aktorRecord.value().identifikatorer.map(Identifikator::toIdent)
        val nyPersonNokkel = brukerIdenterRepository.genererPersonNokkel()
        val kanskjeEksisterendePersonNokler = brukerIdenterRepository.hentTilknyttedePersoner(identer.map { it.ident })

        brukerIdenterRepository.lagre(
            personNokkel = nyPersonNokkel,
            identifikatorer = identer,
            slettEksisterendePersonNokler = kanskjeEksisterendePersonNokler
        )
    }
}

fun Identifikator.toIdent(): Ident {
    return this.let {
        Ident(
            ident = Id.of(it.idnummer.toString()),
            historisk = !it.gjeldende,
            gruppe = Gruppe.valueOf(it.type.toString()),
        )
    }
}