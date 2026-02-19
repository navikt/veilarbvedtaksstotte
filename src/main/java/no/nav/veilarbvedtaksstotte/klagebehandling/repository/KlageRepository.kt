package no.nav.veilarbvedtaksstotte.klagebehandling.repository

import no.nav.veilarbvedtaksstotte.klagebehandling.domene.KlageBehandling
import no.nav.veilarbvedtaksstotte.klagebehandling.domene.dto.OpprettKlageRequest
import no.nav.veilarbvedtaksstotte.utils.SecureLog.secureLog
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class KlageRepository(private val db: JdbcTemplate) {

    fun upsertKlageBakgrunnsdata(
        klageRequest: OpprettKlageRequest
    ) {
        val sql = """
            INSERT INTO $KLAGE_TABLE ($VEDTAK_ID, $VEILEDER_IDENT, $NORSK_IDENT, $TIDSPUNKT_START_KLAGEBEHANDLING, $RAD_SIST_ENDRET)
            VALUES (?,?,?,current_timestamp, current_timestamp)
            ON CONFLICT ($VEDTAK_ID) 
            DO UPDATE SET 
            VEILEDER_IDENT = EXCLUDED.${VEILEDER_IDENT},
            NORSK_IDENT = EXCLUDED.${NORSK_IDENT},
            RAD_SIST_ENDRET = current_timestamp 
        """.trimIndent()
        try {
            db.update(sql, klageRequest.vedtakId, klageRequest.veilederIdent, klageRequest.fnr.get())
        } catch (ex: Exception) {
            secureLog.error(
                "Kunne ikke lagre klagebehandling for vedtakId: ${klageRequest.vedtakId}, feil: {}",
                ex
            )
        }
    }

    fun upsertKlageBrukerdata(
        vedtakid: Long,
        klageDato: LocalDate?,
        klageBegrunnelse: String?
    ) {
        val sql = """
                UPDATE $KLAGE_TABLE SET
                    $BRUKER_KLAGE_DATO = ?,
                    $BRUKER_KLAGE_BEGRUNNELSE = ?,
                    $RAD_SIST_ENDRET = current_timestamp
                WHERE $VEDTAK_ID = ?
            """.trimIndent()

        try {
            db.update(sql, klageDato, klageBegrunnelse, vedtakid)
        } catch (ex: Exception) {
            secureLog.error(
                "Kunne ikke lagre brukerdata for klagebehandling for vedtakId: $vedtakid, feil: {}",
                ex
            )
        }
    }

    fun hentKlageBehandling(vedtakid: Long): KlageBehandling? {
        val sql = "SELECT * FROM $KLAGE_TABLE WHERE $VEDTAK_ID = ?"
        return try {
            db.queryForObject(sql, { rs, _ ->
                KlageBehandling(
                    vedtakId = rs.getLong(VEDTAK_ID),
                    veilederIdent = rs.getString(VEILEDER_IDENT),
                    norskIdent = rs.getString(NORSK_IDENT),
                    klageDato = rs.getDate(BRUKER_KLAGE_DATO)?.toLocalDate(),
                    klageBegrunnelse = rs.getString(BRUKER_KLAGE_BEGRUNNELSE)
                )
            }, vedtakid)
        } catch (ex: Exception) {
            secureLog.error(
                "Kunne ikke hente klagebehandling for vedtakId: $vedtakid",
            )
            null
        }
    }

    companion object {
        const val KLAGE_TABLE: String = "KLAGE"
        private const val VEDTAK_ID = "VEDTAK_ID"
        private const val VEILEDER_IDENT = "VEILEDER_IDENT"
        private const val NORSK_IDENT = "NORSK_IDENT"
        private const val BRUKER_KLAGE_DATO = "BRUKER_KLAGE_DATO"
        private const val BRUKER_KLAGE_BEGRUNNELSE = "BRUKER_KLAGE_BEGRUNNELSE"
        private const val TIDSPUNKT_START_KLAGEBEHANDLING = "TIDSPUNKT_START_KLAGEBEHANDLING"
        private const val RAD_SIST_ENDRET = "RAD_SIST_ENDRET"
    }
}

