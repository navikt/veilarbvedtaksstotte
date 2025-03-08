package no.nav.veilarbvedtaksstotte.service

import no.nav.common.types.identer.*
import no.nav.person.pdl.aktor.v2.Aktor
import no.nav.person.pdl.aktor.v2.Identifikator
import no.nav.person.pdl.aktor.v2.Type
import no.nav.veilarbvedtaksstotte.IntegrationTestBase
import no.nav.veilarbvedtaksstotte.domain.Gruppe
import no.nav.veilarbvedtaksstotte.domain.IdentDetaljer
import no.nav.veilarbvedtaksstotte.domain.PersonMedIdenter
import no.nav.veilarbvedtaksstotte.repository.BrukerIdenterRepository
import no.nav.veilarbvedtaksstotte.service.BrukerIdenterService.Companion.toIdent
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.queryForObject
import java.util.stream.Stream
import kotlin.random.Random

class BrukerIdenterServiceTest(
    @Autowired val jdbcTemplate: JdbcTemplate,
    @Autowired val brukerIdenterService: BrukerIdenterService,
    @Autowired val brukerIdenterRepository: BrukerIdenterRepository
) : IntegrationTestBase() {

    @BeforeEach
    fun reset() {
        jdbcTemplate.update("TRUNCATE TABLE bruker_identer")
        jdbcTemplate.update("ALTER SEQUENCE bruker_identer_person_seq RESTART WITH 1")
    }

    @ParameterizedTest
    @MethodSource("aktorIdOgAktorSource")
    fun `skal lagre alle identifikatorer når vi ikke har lagret noe for personen fra før`(
        kafkaRecord: ConsumerRecord<String?, Aktor?>,
        forventetResultat: List<PersonMedIdenter>
    ) {
        // When
        brukerIdenterService.behandlePdlAktorV2Melding(kafkaRecord)

        // Then
        val sql = """
            SELECT bi2.* FROM bruker_identer bi1
            INNER JOIN bruker_identer bi2 ON bi1.person = bi2.person
            WHERE bi1.ident = ?
        """.trimIndent()
        val personMedIdenter =
            jdbcTemplate.query(sql, { rs, _ ->
                PersonMedIdenter(
                    personNokkel = rs.getString("person"),
                    identDetaljer = IdentDetaljer(
                        ident = EksternBrukerId.of(rs.getString("ident")),
                        historisk = rs.getBoolean("historisk"),
                        gruppe = Gruppe.valueOf(rs.getString("gruppe")),
                    )
                )
            }, kafkaRecord.key())
        assertThat(personMedIdenter).containsExactlyInAnyOrderElementsOf(forventetResultat)
    }

    @Test
    fun `skal slette alle identer knyttet til person når det kommer ny melding for samme person`() {
        // Given
        val tidligereKafkaRecord = genererRandomPdlAktorV2TopicConsumerRecord()
        val tidligerePersonSekvensVerdi = brukerIdenterRepository.genererPersonNokkel()
        brukerIdenterRepository.lagre(
            tidligerePersonSekvensVerdi,
            tidligereKafkaRecord.value()!!.identifikatorer.map(::toIdent)
        )

        // When
        val nyKafkaRecord = ConsumerRecord(
            "pdl.aktor-v2",
            0,
            0,
            tidligereKafkaRecord.key(),
            genererRandomPdlAktorV2TopicConsumerRecord(recordKey = tidligereKafkaRecord.key()!!).value()
        )
        brukerIdenterService.behandlePdlAktorV2Melding(nyKafkaRecord)

        // Then
        val antallRaderSql = "SELECT COUNT(*) FROM bruker_identer WHERE person = ?"
        val antallRaderTidligerePersonFaktisk =
            jdbcTemplate.queryForObject(antallRaderSql, Int::class.java, tidligerePersonSekvensVerdi)
        val antallRaderTidligerePersonForventet = 0
        val hentAlleIdenterForPersonSql = """
            SELECT bi2.* FROM bruker_identer bi1
            INNER JOIN bruker_identer bi2 on bi1.person = bi2.person
            WHERE bi1.ident = ?
            """.trimIndent()
        val identerNyPerson = jdbcTemplate.query(hentAlleIdenterForPersonSql, { rs, _ ->
            PersonMedIdenter(
                personNokkel = rs.getString("person"),
                identDetaljer = IdentDetaljer(
                    ident = EksternBrukerId.of(rs.getString("ident")),
                    historisk = rs.getBoolean("historisk"),
                    gruppe = Gruppe.valueOf(rs.getString("gruppe")),
                )
            )
        }, nyKafkaRecord.key())
        val antallPersonNoklerEtterBehandlingFaktisk = identerNyPerson.map { it.personNokkel }.toSet().size
        val antallPersonNoklerEtterBehandlingForventet = 1
        val alleIdenterForPersonEtterBehandlingFaktisk = identerNyPerson.map { it.identDetaljer }
        val alleIdenterForPersonEtterBehandlingForventet =
            nyKafkaRecord.value()!!.identifikatorer.map { toIdent(it) }

        assertThat(antallRaderTidligerePersonFaktisk!!).isEqualTo(antallRaderTidligerePersonForventet)
        assertThat(antallPersonNoklerEtterBehandlingFaktisk).isEqualTo(antallPersonNoklerEtterBehandlingForventet)
        assertThat(alleIdenterForPersonEtterBehandlingFaktisk).containsExactlyInAnyOrderElementsOf(
            alleIdenterForPersonEtterBehandlingForventet
        )
    }

    @Test
    fun `skal slette alle personer knyttet til identer i melding når det kommer ny melding, og lagre identene på ny person`() {
        // Given
        val tidligerePersonSekvensVerdiPerson1 = brukerIdenterRepository.genererPersonNokkel()
        val tidligerePersonSekvensVerdiPerson2 = brukerIdenterRepository.genererPersonNokkel()
        val tidligereKafkaRecordPerson1 = genererRandomPdlAktorV2TopicConsumerRecord()
        val tidligereKafkaRecordPerson2 = genererRandomPdlAktorV2TopicConsumerRecord()
        brukerIdenterRepository.lagre(
            tidligerePersonSekvensVerdiPerson1,
            tidligereKafkaRecordPerson1.value()!!.identifikatorer.map(::toIdent)
        )
        brukerIdenterRepository.lagre(
            tidligerePersonSekvensVerdiPerson2,
            tidligereKafkaRecordPerson2.value()!!.identifikatorer.map(::toIdent)
        )

        // When
        val nyKafkaRecordKey = genererRandomAktorId().get()
        val nyKafkaRecord = ConsumerRecord(
            "pdl.aktor-v2",
            0,
            0,
            nyKafkaRecordKey,
            Aktor(
                buildList {
                    addAll(genererRandomPdlAktorV2TopicConsumerRecord(recordKey = nyKafkaRecordKey).value()!!.identifikatorer)
                    add(tidligereKafkaRecordPerson1.value()!!.identifikatorer.first())
                    add(tidligereKafkaRecordPerson2.value()!!.identifikatorer.first())
                }
            )
        )
        brukerIdenterService.behandlePdlAktorV2Melding(nyKafkaRecord)

        // Then
        val antallRaderSql = "SELECT COUNT(*) FROM bruker_identer WHERE person = ?"
        val antallRaderTidligerePerson1Faktisk =
            jdbcTemplate.queryForObject(antallRaderSql, Int::class.java, tidligerePersonSekvensVerdiPerson1)
        val antallRaderTidligerePerson1Forventet = 0
        val antallRaderTidligerePerson2Faktisk =
            jdbcTemplate.queryForObject(antallRaderSql, Int::class.java, tidligerePersonSekvensVerdiPerson2)
        val antallRaderTidligerePerson2Forventet = 0
        val hentAlleIdenterForPersonSql = """
            SELECT bi2.* FROM bruker_identer bi1
            INNER JOIN bruker_identer bi2 on bi1.person = bi2.person
            WHERE bi1.ident = ?
            """.trimIndent()
        val identerNyPerson = jdbcTemplate.query(hentAlleIdenterForPersonSql, { rs, _ ->
            PersonMedIdenter(
                personNokkel = rs.getString("person"),
                identDetaljer = IdentDetaljer(
                    ident = EksternBrukerId.of(rs.getString("ident")),
                    historisk = rs.getBoolean("historisk"),
                    gruppe = Gruppe.valueOf(rs.getString("gruppe")),
                )
            )
        }, nyKafkaRecord.key())
        val antallPersonNoklerEtterBehandlingFaktisk = identerNyPerson.map { it.personNokkel }.toSet().size
        val antallPersonNoklerEtterBehandlingForventet = 1
        val alleIdenterForPersonEtterBehandlingFaktisk = identerNyPerson.map { it.identDetaljer }
        val alleIdenterForPersonEtterBehandlingForventet =
            nyKafkaRecord.value().identifikatorer.map { toIdent(it) }

        assertThat(antallRaderTidligerePerson1Faktisk!!).isEqualTo(antallRaderTidligerePerson1Forventet)
        assertThat(antallRaderTidligerePerson2Faktisk!!).isEqualTo(antallRaderTidligerePerson2Forventet)
        assertThat(antallPersonNoklerEtterBehandlingFaktisk).isEqualTo(antallPersonNoklerEtterBehandlingForventet)
        assertThat(alleIdenterForPersonEtterBehandlingFaktisk).containsExactlyInAnyOrderElementsOf(
            alleIdenterForPersonEtterBehandlingForventet
        )
    }

    @Test
    fun `samme person og ident skal ikke kunne lagres flere ganger`() {
        // Given
        val randomAktorIdIdentifikator = genererRandomIdentifikator(type = Type.AKTORID)
        val randomFolkeregisteridentIdentifikator = genererRandomIdentifikator(type = Type.FOLKEREGISTERIDENT)
        val identifikatorer = listOf(
            randomAktorIdIdentifikator,
            randomAktorIdIdentifikator,
            randomFolkeregisteridentIdentifikator
        )
        val aktorRecord = ConsumerRecord("pdl.aktor-v2", 0, 1, genererRandomAktorId().get(), Aktor(identifikatorer))

        // When/Then
        assertThrows<RuntimeException> { brukerIdenterService.behandlePdlAktorV2Melding(aktorRecord) }
    }

    @Test
    fun `skal slette ident knyttet til kafka record key dersom vi mottar tombstone`() {
        // Given
        val aktorRecordKey = genererRandomAktorId().get()
        val opprinneligAktorRecord = genererRandomPdlAktorV2TopicConsumerRecord(recordKey = aktorRecordKey)
        val opprinnligPersonNokkel = brukerIdenterRepository.genererPersonNokkel()
        brukerIdenterRepository.lagre(
            personNokkel = opprinnligPersonNokkel,
            identifikatorer = opprinneligAktorRecord.value()!!.identifikatorer.map(::toIdent)
        )

        // When
        val tombstoneAktorRecord =
            genererRandomPdlAktorV2TopicConsumerRecord(recordKey = aktorRecordKey, tombstone = true)
        brukerIdenterService.behandlePdlAktorV2Melding(tombstoneAktorRecord)

        // Then
        val antallRaderSql = "SELECT COUNT(*) FROM bruker_identer WHERE ident = ?"
        val antallRaderOpprinneligIdentifikatorFaktisk =
            jdbcTemplate.queryForObject(antallRaderSql, Int::class.java, aktorRecordKey)
        val antallRaderOpprinneligIdentifikatorForventet = 0
        assertThat(antallRaderOpprinneligIdentifikatorFaktisk!!).isEqualTo(antallRaderOpprinneligIdentifikatorForventet)
    }

    @Test
    fun `skal slette alle personer knyttet til kafka record key dersom vi mottar tombstone`() {
        // Given
        val aktorRecordKey = genererRandomAktorId().get()
        val opprinneligAktorRecord =
            genererRandomPdlAktorV2TopicConsumerRecord(recordKey = aktorRecordKey, antallHistoriskeIdenter = 2)
        val opprinneligPersonNokkel = brukerIdenterRepository.genererPersonNokkel()
        brukerIdenterRepository.lagre(
            personNokkel = opprinneligPersonNokkel,
            identifikatorer = opprinneligAktorRecord.value()!!.identifikatorer.map(::toIdent)
        )

        // When
        val tombstoneAktorRecord =
            genererRandomPdlAktorV2TopicConsumerRecord(recordKey = aktorRecordKey, tombstone = true)
        brukerIdenterService.behandlePdlAktorV2Melding(tombstoneAktorRecord)

        // Then
        val antallRaderForPersonSql = "SELECT COUNT(*) FROM bruker_identer WHERE person = ?"
        val antallRaderTotaltSql = "SELECT COUNT(*) FROM bruker_identer"
        val antallRaderOpprinneligPersonFaktisk =
            jdbcTemplate.queryForObject(antallRaderForPersonSql, Int::class.java, opprinneligPersonNokkel)
        val antallRaderTotaltFaktisk = jdbcTemplate.queryForObject(antallRaderTotaltSql, Int::class.java)
        val antallRaderOpprinneligPersonForventet = 0
        val antallRaderTotaltForventet = 0
        assertThat(antallRaderOpprinneligPersonFaktisk!!).isEqualTo(antallRaderOpprinneligPersonForventet)
        assertThat(antallRaderTotaltFaktisk!!).isEqualTo(antallRaderTotaltForventet)
    }

    @Test
    fun `melding med key på uventet format skal feile validering og resultere i exception`() {
        // Given
        val ukjentFormatAktorRecord = ConsumerRecord(
            "pdl.aktor-v2",
            0,
            1,
            AktorId.of("1").get(),
            Aktor(listOf(genererRandomIdentifikator()))
        )

        // When/Then
        assertThrows<BrukerIdenterValideringException> {
            brukerIdenterService.behandlePdlAktorV2Melding(
                ukjentFormatAktorRecord
            )
        }
    }

    @Test
    fun `melding med value på uventet format skal feile validering og resultere i exception`() {
        // Given
        val ukjentFormatAktorRecord = ConsumerRecord(
            "pdl.aktor-v2",
            0,
            1,
            genererRandomAktorId().get(),
            Aktor(null)
        )

        // When/Then
        assertThrows<BrukerIdenterValideringException> {
            brukerIdenterService.behandlePdlAktorV2Melding(
                ukjentFormatAktorRecord
            )
        }
    }

    companion object {
        @JvmStatic
        private fun aktorIdOgAktorSource(): Stream<Arguments> {
            val aktor1 =
                AktorId.of("1111111111111") to Aktor(
                    listOf(
                        Identifikator("1111111111111", Type.AKTORID, true)
                    )
                )
            val personIdenter1 = listOf(
                PersonMedIdenter(
                    personNokkel = "1", IdentDetaljer(
                        ident = aktor1.first,
                        historisk = false,
                        gruppe = Gruppe.AKTORID,
                    )
                )
            )

            val aktor2 = AktorId.of("2222222222222") to Aktor(
                listOf(
                    Identifikator("2222222222222", Type.AKTORID, true),
                    Identifikator("22222222222", Type.FOLKEREGISTERIDENT, true)
                )
            )
            val personIdenter2s = listOf(
                PersonMedIdenter(
                    personNokkel = "1", IdentDetaljer(
                        ident = AktorId.of("2222222222222"),
                        historisk = false,
                        gruppe = Gruppe.AKTORID
                    )
                ),
                PersonMedIdenter(
                    personNokkel = "1", IdentDetaljer(
                        ident = Fnr.of("22222222222"),
                        historisk = false,
                        gruppe = Gruppe.FOLKEREGISTERIDENT
                    )
                )
            )

            val aktor3 =
                AktorId.of("4444444444444") to Aktor(
                    listOf(
                        Identifikator("4444444444444", Type.AKTORID, true),
                        Identifikator("3333333333333", Type.AKTORID, false),
                        Identifikator("44444444444", Type.FOLKEREGISTERIDENT, true),
                        Identifikator("33333333333", Type.FOLKEREGISTERIDENT, false)
                    )
                )
            val personIdenter3s = listOf(
                PersonMedIdenter(
                    personNokkel = "1", IdentDetaljer(
                        ident = AktorId.of("4444444444444"),
                        historisk = false,
                        gruppe = Gruppe.AKTORID
                    )
                ),
                PersonMedIdenter(
                    personNokkel = "1", IdentDetaljer(
                        ident = AktorId.of("3333333333333"),
                        historisk = true,
                        gruppe = Gruppe.AKTORID
                    )
                ),
                PersonMedIdenter(
                    personNokkel = "1", IdentDetaljer(
                        ident = Fnr.of("44444444444"),
                        historisk = false,
                        gruppe = Gruppe.FOLKEREGISTERIDENT
                    )
                ),
                PersonMedIdenter(
                    personNokkel = "1", IdentDetaljer(
                        ident = Fnr.of("33333333333"),
                        historisk = true,
                        gruppe = Gruppe.FOLKEREGISTERIDENT
                    )
                )
            )

            return Stream.of(
                Arguments.of(
                    ConsumerRecord("pdl.aktor-v2", 0, 0, aktor1.first.get(), aktor1.second),
                    personIdenter1
                ),
                Arguments.of(
                    ConsumerRecord("pdl.aktor-v2", 0, 1, aktor2.first.get(), aktor2.second),
                    personIdenter2s
                ),
                Arguments.of(
                    ConsumerRecord("pdl.aktor-v2", 0, 2, aktor3.first.get(), aktor3.second),
                    personIdenter3s
                ),
            )
        }

        private fun genererRandomPdlAktorV2TopicConsumerRecord(
            recordKey: String = genererRandomAktorId().get(),
            tombstone: Boolean = false,
            antallHistoriskeIdenter: Int = 0,
            offset: Long = 0,
            partition: Int = 0,
        ): ConsumerRecord<String?, Aktor?> {
            return if (tombstone) {
                ConsumerRecord("pdl.aktor-v2", partition, offset, recordKey, null)
            } else {
                val fnr = genererRandomNorskIdent()
                val identifikatorer = buildList {
                    add(Identifikator(recordKey, Type.AKTORID, true))
                    add(Identifikator(fnr.get(), Type.FOLKEREGISTERIDENT, true))
                    addAll((1..antallHistoriskeIdenter).map { genererRandomIdentifikator(historisk = true) })
                }
                val recordValue = Aktor(identifikatorer)
                ConsumerRecord("pdl.aktor-v2", partition, offset, recordKey, recordValue)
            }
        }

        private fun genererRandomIdentifikator(
            type: Type = getRandomType(),
            historisk: Boolean = false
        ): Identifikator {
            return Identifikator(
                when (type) {
                    Type.AKTORID -> genererRandomAktorId().get()
                    Type.FOLKEREGISTERIDENT -> genererRandomNorskIdent().get()
                    Type.NPID -> genererRandomId().get()
                },
                type,
                !historisk
            )
        }

        private fun getRandomType(): Type {
            return Type.entries[Random.nextInt(0, Type.entries.size)]
        }

        private fun genererRandomAktorId(): AktorId {
            return AktorId.of(genererRandomStringBestaaendeAvTall(13))
        }

        private fun genererRandomNorskIdent(): NorskIdent {
            return NorskIdent.of(genererRandomStringBestaaendeAvTall(11))
        }

        private fun genererRandomId(): Id {
            return Id.of(genererRandomStringBestaaendeAvTall(11))
        }

        private fun genererRandomStringBestaaendeAvTall(antallSiffer: Int): String {
            if (antallSiffer <= 0) throw IllegalArgumentException("Antall siffer må være større enn 0.")

            return (1..antallSiffer)
                .map { Random.nextInt(0, 10) }
                .joinToString("")
        }
    }
}