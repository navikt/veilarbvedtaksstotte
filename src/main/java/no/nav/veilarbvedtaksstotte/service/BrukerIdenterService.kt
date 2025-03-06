package no.nav.veilarbvedtaksstotte.service

import io.micrometer.core.annotation.Timed
import lombok.extern.slf4j.Slf4j
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Id
import no.nav.person.pdl.aktor.v2.Aktor
import no.nav.person.pdl.aktor.v2.Identifikator
import no.nav.person.pdl.aktor.v2.Type
import no.nav.veilarbvedtaksstotte.domain.Gruppe
import no.nav.veilarbvedtaksstotte.domain.IdentDetaljer
import no.nav.veilarbvedtaksstotte.repository.BrukerIdenterRepository
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
    @Timed(value = "obo.veilarbvedtaksstotte.kafka.pdl-aktor-v2.processing-time", description = "Time spent by veilarbvedtaksstotte processing Kafka-records on the pdl.aktor-v2 topic")
    fun behandlePdlAktorV2Melding(aktorRecord: ConsumerRecord<String?, Aktor?>) {
        logger.info("Behandler melding: topic ${aktorRecord.topic()}, offset ${aktorRecord.offset()}, partisjon ${aktorRecord.partition()}.")

        val validertAktorId = tilValidertAktorId(aktorRecord.key())
        val validertIdentDetaljerListe = tilValidertIdentDetaljerListe(aktorRecord.value())

        if (validertIdentDetaljerListe == null) {
            brukerIdenterRepository.slett(validertAktorId)
            return
        }

        val nyPersonNokkel = brukerIdenterRepository.genererPersonNokkel()
        val eksisterendePersonNokler =
            brukerIdenterRepository.hentTilknyttedePersoner(validertIdentDetaljerListe.map { it.ident })

        if (eksisterendePersonNokler.isNotEmpty()) {
            logger.info(
                "Identer eksisterte fra før med personnøkler: ${
                    eksisterendePersonNokler.joinToString(
                        ","
                    )
                }. Lagrer identer på ny personnøkkel: $nyPersonNokkel."
            )
        } else {
            logger.info("Lagrer identer på personnøkkel: $nyPersonNokkel.")
        }

        brukerIdenterRepository.lagre(
            personNokkel = nyPersonNokkel,
            identifikatorer = validertIdentDetaljerListe,
            slettEksisterendePersonNokler = eksisterendePersonNokler
        )
    }


    companion object {
        private fun tilValidertAktorId(kafkaRecordKey: String?): AktorId {
            return try {
                checkNotNull(kafkaRecordKey) { "'key' var: null. Forventet: en Aktør-ID." }

                check(kafkaRecordKey.length == 13) { "Ugyldig lengde på Aktør-ID: ${kafkaRecordKey.length}. Forventet lengde: 13." }

                AktorId.of(kafkaRecordKey)
            } catch (e: IllegalStateException) {
                throw BrukerIdenterValideringException("Validering av pdl.aktor-v2 consumer record feilet.", e)
            }
        }

        private fun tilValidertIdentDetaljerListe(kafkaRecordValue: Aktor?): List<IdentDetaljer>? {
            if (kafkaRecordValue == null) {
                return null
            }

            return try {
                val identifikatorer = kafkaRecordValue.identifikatorer

                checkNotNull(identifikatorer) { "'identifikatorer' var: null. Forventet: en liste med identifikatorer, eller en tom liste." }

                identifikatorer.forEach {
                    checkNotNull(it.idnummer) { "'idnummer' var: null. Forventet: en string." }
                    checkNotNull(it.type) { "'type' var: null. Forventet: en identifikatortype." }
                }

                identifikatorer.map(::toIdent)
            } catch (e: IllegalStateException) {
                throw BrukerIdenterValideringException("Validering av pdl.aktor-v2 consumer record feilet.", e)
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