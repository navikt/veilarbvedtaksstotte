package no.nav.veilarbvedtaksstotte.repository

import no.nav.veilarbvedtaksstotte.domain.vedtak.KildeEntity
import no.nav.veilarbvedtaksstotte.domain.vedtak.KildeForVedtak
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak
import no.nav.veilarbvedtaksstotte.utils.DbUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.util.*
import kotlin.Int
import kotlin.Long

@Repository
class KilderRepository @Autowired constructor(private val db: JdbcTemplate) {
    fun lagKilder(kilder: List<String?>, vedtakId: Long) {
        kilder.forEach { opplysning: String? -> insertKilde(opplysning, vedtakId) }
    }

    fun hentKilderForVedtak(vedtakId: Long): List<KildeForVedtak?> {
        val sql = "SELECT * FROM $KILDE_TABLE WHERE $VEDTAK_ID = ?"
        return db.query(sql, { rs: ResultSet?, _: Int -> mapKilder(rs!!) }, vedtakId)
    }

    fun hentKilderForAlleVedtak(vedtakListe: List<Vedtak>): List<KildeForVedtak?> {
        if (vedtakListe.isEmpty()) {
            return emptyList()
        }

        val vedtakIder = vedtakListe.map(Vedtak::getId)
        val sql = "SELECT * FROM $KILDE_TABLE WHERE $VEDTAK_ID = SOME(?::bigint[])"
        val strVedtakIder = vedtakIder.map { it.toString() }

        return db.query(
            sql,
            { rs: ResultSet?, _: Int -> mapKilder(rs!!) },
            DbUtils.toPostgresArray(strVedtakIder)
        )
    }

    fun slettKilder(vedtakId: Long) {
        db.update("DELETE FROM $KILDE_TABLE WHERE $VEDTAK_ID = ?", vedtakId)
    }

    private fun insertKilde(tekst: String?, vedtakId: Long) {
        val sql = "INSERT INTO $KILDE_TABLE($VEDTAK_ID, $TEKST, $KILDE_ID) values(?,?,?)"
        db.update(sql, vedtakId, tekst, UUID.randomUUID())
    }

    companion object {
        const val KILDE_TABLE: String = "KILDE"
        private const val VEDTAK_ID = "VEDTAK_ID"
        private const val TEKST = "TEKST"
        private const val KILDE_ID = "KILDE_ID"

        private fun mapKilder(rs: ResultSet): KildeForVedtak {
            val kildeEntity = KildeEntity(
                rs.getString(TEKST),
                rs.getString(KILDE_ID)?.let(UUID::fromString)
            )

            return KildeForVedtak(
                rs.getLong(VEDTAK_ID),
                kildeEntity
            )
        }
    }
}
