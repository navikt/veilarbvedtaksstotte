package no.nav.veilarbvedtaksstotte.repository

import no.nav.common.types.identer.Id
import no.nav.veilarbvedtaksstotte.domain.Gruppe
import no.nav.veilarbvedtaksstotte.domain.IdentDetaljer
import no.nav.veilarbvedtaksstotte.domain.PersonMedIdenter
import no.nav.veilarbvedtaksstotte.domain.PersonNokkel
import no.nav.veilarbvedtaksstotte.utils.DbUtils.toPostgresArray
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class BrukerIdenterRepository(
    @Autowired val jdbcTemplate: JdbcTemplate
) {

    fun hentBrukerIdenter(ident: Id): PersonMedIdenter {
        val personNokkel = hentTilknyttetPerson(ident)
            ?: throw IngenPersonMedIdentException("Fant ingen personnøkkel tilknyttet ident $ident")

        val hentIdentDetaljerSql = "SELECT ident, historisk, gruppe FROM bruker_identer WHERE person = ?"
        val identDetaljer = jdbcTemplate.query(
            hentIdentDetaljerSql,
            { rs, _ ->
                IdentDetaljer(
                    ident = Id(rs.getString("ident")),
                    historisk = rs.getBoolean("historisk"),
                    gruppe = Gruppe.valueOf(rs.getString("gruppe"))
                )
            },
            personNokkel
        ).toSet()

        return PersonMedIdenter(personNokkel = personNokkel, identDetaljer = identDetaljer)
    }

    /**
     * Lagrer en liste med identer på en og samme person gitt ved [personNokkel].
     *
     * @param personNokkel en [PersonNokkel] som [identifikatorer] skal lagres på
     * @param identifikatorer en liste med [IdentDetaljer] som skal lagres på personen
     */
    @Transactional
    fun lagre(
        personNokkel: PersonNokkel,
        identifikatorer: List<IdentDetaljer>
    ) {

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

    fun hentTilknyttetPerson(ident: Id): PersonNokkel? {
        val sql = "SELECT person FROM bruker_identer WHERE ident = ?"
        return jdbcTemplate.queryForObject(
            sql,
            { rs, _ -> rs.getString("person") as PersonNokkel },
            ident
        )
    }

    /**
     * Genererer og returnerer en nøkkel som kan brukes for å knytte flere identer til en og samme person.
     * Nøkkelen kan brukes i forbindelse med lagring.
     *
     * @see [BrukerIdenterRepository.lagre]
     */
    fun genererPersonNokkel(): PersonNokkel {
        val sql = "SELECT nextval('BRUKER_IDENTER_PERSON_SEQ')"
        return jdbcTemplate.queryForObject(sql, PersonNokkel::class.java)
            ?: throw IllegalStateException("Kunne ikke hente ny verdi fra \"BRUKER_IDENTER_PERSON_SEQ\" sekvensen.")
    }

    fun slett(personerNokler: List<PersonNokkel>) {
        val slettSql = "DELETE FROM bruker_identer WHERE person = any(?::varchar[])"
        jdbcTemplate.update(slettSql, toPostgresArray(personerNokler))
    }
}

data class IngenPersonMedIdentException(val melding: String) : RuntimeException(melding)