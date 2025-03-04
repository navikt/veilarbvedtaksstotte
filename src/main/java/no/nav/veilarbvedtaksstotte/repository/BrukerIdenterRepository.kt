package no.nav.veilarbvedtaksstotte.repository

import no.nav.common.types.identer.Id
import no.nav.veilarbvedtaksstotte.utils.DbUtils.toPostgresArray
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class BrukerIdenterRepository(
    @Autowired val jdbcTemplate: JdbcTemplate
) {

    /**
     * Lagrer en liste med identer på en og samme person gitt ved [personNokkel].
     *
     * Dersom [slettEksisterendePersonNokler] inneholder en eller flere [PersonNokkel] vil alle innslag på disse nøklene slettes
     * først, før [identifikatorer] lagres på [personNokkel].
     *
     * @param personNokkel en [PersonNokkel] som [identifikatorer] skal lagres på
     * @param identifikatorer en liste med [Ident] som skal lagres på personen
     * @param slettEksisterendePersonNokler en liste med [PersonNokkel] som skal slettes, default er en tom liste
     */
    @Transactional
    fun lagre(
        personNokkel: PersonNokkel,
        identifikatorer: List<Ident>,
        slettEksisterendePersonNokler: List<PersonNokkel> = emptyList()
    ) {

        if (slettEksisterendePersonNokler.isNotEmpty()) {
            val slettSql = "DELETE FROM bruker_identer WHERE person = any(?::varchar[])"
            jdbcTemplate.update(slettSql, toPostgresArray(slettEksisterendePersonNokler))
        }

        val identifikatorerArguments = identifikatorer.map {
            arrayOf(personNokkel, it.ident.toString(), it.historisk, it.gruppe.toString())
        }

        val insertSql = "INSERT INTO bruker_identer(person, ident, historisk, gruppe) VALUES (?, ?, ?, ?)"
        jdbcTemplate.batchUpdate(insertSql, identifikatorerArguments)
    }

    fun hentTilknyttedePersoner(identer: List<Id>): List<PersonNokkel> {
        val sql = "SELECT person FROM bruker_identer WHERE ident = ANY(?::VARCHAR[])"
        return jdbcTemplate.query(
            sql,
            { rs, _ -> rs.getString("person") },
            toPostgresArray(identer.map { it.get() })
        )
    }

    /**
     * Genererer og returnerer en nøkkel som kan brukes for å knytte flere identer til en og samme person.
     * Nøkkelen kan brukes i forbindelse med lagring.
     *
     * @see [BrukerIdenterRepository.lagre]
     */
    fun genererPersonNokkel(): PersonNokkel {
        val sql = "SELECT nextval('BRUKER_IDENTER_PERSON_SEQ')";
        return jdbcTemplate.queryForObject(sql, PersonNokkel::class.java)
            ?: throw IllegalStateException("Kunne ikke hente ny verdi fra \"BRUKER_IDENTER_PERSON_SEQ\" sekvensen.")
    }
}

/**
 * Representerer mapping-nøkkelen vi bruker internt for å koble flere identer (AktørID, fødselsnummer, osv.) til en
 * og samme fysiske person. Typen er et alias for [String], for å gjøre den mer meningsbærende.
 */
typealias PersonNokkel = String

enum class Gruppe {
    FOLKEREGISTERIDENT,
    AKTORID,
    NPID
}

data class Ident(
    val ident: Id,
    val historisk: Boolean,
    val gruppe: Gruppe
)

data class PersonMedIdenter(
    val personNokkel: PersonNokkel,
    val ident: Ident
)