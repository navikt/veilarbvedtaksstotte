package no.nav.veilarbvedtaksstotte.service

import lombok.extern.slf4j.Slf4j
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Id
import no.nav.person.pdl.aktor.v2.Aktor
import no.nav.person.pdl.aktor.v2.Identifikator
import no.nav.person.pdl.aktor.v2.Type
import no.nav.veilarbvedtaksstotte.repository.BrukerIdenterRepository
import no.nav.veilarbvedtaksstotte.domain.Gruppe
import no.nav.veilarbvedtaksstotte.domain.IdentDetaljer
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
        logger.info("Behandler melding: topic ${aktorRecord.topic()}, offset ${aktorRecord.offset()}, partisjon ${aktorRecord.partition()}.")

        valider(aktorRecord)

        val identer = aktorRecord.value().identifikatorer.map(::toIdent)
        val nyPersonNokkel = brukerIdenterRepository.genererPersonNokkel()
        val kanskjeEksisterendePersonNokler = brukerIdenterRepository.hentTilknyttedePersoner(identer.map { it.ident })

        if (kanskjeEksisterendePersonNokler.isNotEmpty()) {
            logger.info("Identer eksisterte fra før med personnøkler: ${kanskjeEksisterendePersonNokler.joinToString(",")}. Lagrer identer på ny personnøkkel: $nyPersonNokkel.")
        } else {
            logger.info("Lagrer identer på personnøkkel: $nyPersonNokkel.")
        }

        brukerIdenterRepository.lagre(
            personNokkel = nyPersonNokkel,
            identifikatorer = identer,
            slettEksisterendePersonNokler = kanskjeEksisterendePersonNokler
        )
    }


    companion object {
        private fun valider(aktorRecord: ConsumerRecord<AktorId, Aktor>) {
            try {
                valider(aktorRecord.key())
                valider(aktorRecord.value())
            } catch (e: IllegalStateException) {
                throw BrukerIdenterValideringException("Validering av pdl.aktor-v2 consumer record feilet.", e)
            }
        }

        private fun valider(aktorId: AktorId) {
            val aktorIdString = aktorId.get()
            check(aktorIdString.length == 13) { "Ugyldig lengde på Aktør-ID: ${aktorIdString.length}. Forventet lengde: 13." }
        }

        private fun valider(aktor: Aktor) {
            val identifikatorer = aktor.identifikatorer

            checkNotNull(identifikatorer) { "'identifikatorer' var: null. Forventet: en liste med identifikatorer, eller en tom liste." }

            identifikatorer.forEach {
                checkNotNull(it.idnummer) { "'idnummer' var: null. Forventet: en string." }
                checkNotNull(it.type) { "'type' var: null. Forventet: en identifikatortype." }
            }
        }

        fun toIdent(identifikator: Identifikator): IdentDetaljer {
            return identifikator.let {
                IdentDetaljer(
                    ident = Id.of(it.idnummer.toString()),
                    historisk = !it.gjeldende,
                    gruppe = when (it.type) {
                        Type.FOLKEREGISTERIDENT -> Gruppe.FOLKEREGISTERIDENT
                        Type.AKTORID -> Gruppe.AKTORID
                        Type.NPID -> Gruppe.NPID
                        else -> Gruppe.UKJENT
                    }
                )
            }
        }
    }
}

data class BrukerIdenterValideringException(val melding: String, val aarsak: Throwable) : Throwable(melding, aarsak)