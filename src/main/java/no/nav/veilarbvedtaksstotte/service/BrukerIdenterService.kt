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
import no.nav.veilarbvedtaksstotte.domain.PersonNokkel
import no.nav.veilarbvedtaksstotte.repository.BrukerIdenterRepository
import no.nav.veilarbvedtaksstotte.utils.SecureLog.secureLog
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

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
    @Timed(
        value = "obo.veilarbvedtaksstotte.kafka.pdl-aktor-v2.processing-time",
        description = "Time spent by veilarbvedtaksstotte processing Kafka-records on the pdl.aktor-v2 topic"
    )
    fun behandlePdlAktorV2Melding(aktorRecord: ConsumerRecord<String?, Aktor?>) {
        logger.info("Behandler melding. Topic: ${aktorRecord.topic()}, offset: ${aktorRecord.offset()}, partisjon: ${aktorRecord.partition()}.")
        secureLog.info("Behandler melding. Topic: ${aktorRecord.topic()}, offset: ${aktorRecord.offset()}, partisjon: ${aktorRecord.partition()}, key: ${aktorRecord.key()}.")

        Person(
            aktorId = tilValidertAktorId(aktorRecord.key()),
            identDetaljerListe = tilValidertIdentDetaljerListe(aktorRecord.value())
        )
            .let(::hentEksisterendePersonNokler)
            .also(::slettIdenterKnyttetTilPersonNokler)
            .also(::lagreIdenterPaNyPersonNokkel)
    }

    /**
     * Henter eksisterende personnøkler, hvis noen, knyttet til personen vi mottok melding på:
     *
     * * dersom det var en "tombstone" melding (dvs. `person.identDetaljerListe` er `null`) forsøker vi å hente personnøkler knyttet til `person.aktorId`
     * * ellers forsøker vi å hente personnøkler knyttet til alle [Id]-ene i `person.identDetaljerListe`
     */
    private fun hentEksisterendePersonNokler(person: Person): Person {
        return if (person.identDetaljerListe == null) {
            // Det er en "tombstone"-melding
            brukerIdenterRepository.hentTilknyttedePersoner(listOf(person.aktorId))
                .also {
                    logger.info("Mottok tombstone-melding. Hentet ${it.size} eksisterende personnøkler knyttet til Kafka-key (Aktør-ID).")
                }
        } else {
            brukerIdenterRepository.hentTilknyttedePersoner(person.identDetaljerListe.map { it.ident })
                .also {
                    logger.info("Hentet ${it.size} eksisterende personnøkler knyttet til identer i Kafka-melding.")
                }
        }.let {
            person.copy(eksisterendePersonNokler = it)
        }
    }

    /**
     * Sletter alle identer knyttet til [Person.eksisterendePersonNokler], hvis noen
     */
    private fun slettIdenterKnyttetTilPersonNokler(person: Person) {
        if (person.eksisterendePersonNokler.isNotEmpty()) {
            val slettedePersonNoklerString = person.eksisterendePersonNokler.joinToString(",")
            brukerIdenterRepository.slett(person.eksisterendePersonNokler)
            logger.info("Slettet eksisterende identer knyttet til personnøkler: $slettedePersonNoklerString.")
        }
    }

    /**
     * Lagrer alle [Person.identDetaljerListe] på en ny personnøkkel, hvis noen
     */
    private fun lagreIdenterPaNyPersonNokkel(
        person: Person
    ) {
        if (person.identDetaljerListe == null) {
            // Siden det var en "tombstone" melding er det ingen identer å lagre, og vi er derfor ferdige
            return
        }

        val nyPersonNokkel = brukerIdenterRepository.genererPersonNokkel()
        brukerIdenterRepository.lagre(
            personNokkel = nyPersonNokkel,
            identifikatorer = person.identDetaljerListe
        )
        logger.info("Lagret identer på ny personnøkkel: $nyPersonNokkel.")
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

                checkNotNull(identifikatorer) { "'identifikatorer' var: null. Forventet: en liste med identifikatorer." }
                check(identifikatorer.size > 0) { "'identifikatorer' var tom. Forventet: minst en identifikator." }

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

/**
 * Value-object for å kunne samle Aktør-ID (fra Kafka-key), identer (fra Kafka-Value) samt eksisterende personnøkler (fra DB).
 */
data class Person(
    val aktorId: AktorId,
    val identDetaljerListe: List<IdentDetaljer>?,
    val eksisterendePersonNokler: List<PersonNokkel> = emptyList()
)

data class BrukerIdenterValideringException(val melding: String, val aarsak: Throwable) : Throwable(melding, aarsak)
