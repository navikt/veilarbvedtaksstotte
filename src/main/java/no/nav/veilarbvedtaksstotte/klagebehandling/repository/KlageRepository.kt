package no.nav.veilarbvedtaksstotte.klagebehandling.repository

import no.nav.veilarbvedtaksstotte.klagebehandling.domene.FormkravOppfylt
import no.nav.veilarbvedtaksstotte.klagebehandling.domene.KlageBehandling
import no.nav.veilarbvedtaksstotte.klagebehandling.domene.Resultat
import no.nav.veilarbvedtaksstotte.klagebehandling.domene.dto.OpprettKlageRequest
import no.nav.veilarbvedtaksstotte.utils.SecureLog.secureLog
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class KlageRepository(private val db: JdbcTemplate) {

    fun upsertOpprettKlagebehandling(
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
        klageDato: LocalDate,
        klageJournalpostid: String,
    ) {
        val sql = """
                UPDATE $KLAGE_TABLE SET
                    $KLAGE_DATO = ?,
                    $KLAGE_JOURNALPOST_ID = ?,
                    $RAD_SIST_ENDRET = current_timestamp
                WHERE $VEDTAK_ID = ?
            """.trimIndent()

        try {
            db.update(sql, klageDato, klageJournalpostid, vedtakid)
        } catch (ex: Exception) {
            secureLog.error(
                "Kunne ikke lagre brukerdata for klagebehandling for vedtakId: $vedtakid, feil: {}",
                ex
            )
        }
    }

    fun upsertFormkrav(
        vedtakid: Long,
        formkravOppfylt: FormkravOppfylt,
        formkravBegrunnelse: String?
    ) {
        val sql = """
                UPDATE $KLAGE_TABLE SET
                    $FORMKRAV_OPPFYLT = ?,
                    $FORMKRAV_BEGRUNNELSE = ?,
                    $RAD_SIST_ENDRET = current_timestamp
                WHERE $VEDTAK_ID = ?
            """.trimIndent()

        try {
            db.update(sql, formkravOppfylt.toString(), formkravBegrunnelse, vedtakid)
        } catch (ex: Exception) {
            secureLog.error(
                "Kunne ikke lagre formkrav for klagebehandling for vedtakId: $vedtakid, feil: {}",
                ex
            )
        }

    }

    fun upsertResultat(
        vedtakid: Long,
        resultat: Resultat,
        resultatBegrunnelse: String?
    ) {
        val sql = """
                UPDATE $KLAGE_TABLE SET
                    $RESULTAT = ?,
                    $RESULTAT_BEGRUNNELSE = ?,
                    $TIDSPUNKT_RESULTAT = current_timestamp,
                    $RAD_SIST_ENDRET = current_timestamp
                WHERE $VEDTAK_ID = ?
            """.trimIndent()

        try {
            db.update(sql, resultat.toString(), resultatBegrunnelse, vedtakid)
        } catch (ex: Exception) {
            secureLog.error(
                "Kunne ikke lagre resultat for klagebehandling for vedtakId: $vedtakid, feil: {}",
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
                    klageDato = rs.getDate(KLAGE_DATO)?.toLocalDate(),
                    klageJournalpostid = rs.getString(KLAGE_JOURNALPOST_ID),
                    formkravOppfylt = rs.getString(FORMKRAV_OPPFYLT)?.let { FormkravOppfylt.valueOf(it) },
                    formkravBegrunnelse = rs.getString(FORMKRAV_BEGRUNNELSE),
                    resultat = rs.getString(RESULTAT)?.let { Resultat.valueOf(it) },
                    resultatBegrunnelse = rs.getString(RESULTAT_BEGRUNNELSE)
                )
            }, vedtakid)
        } catch (ex: Exception) {
            secureLog.error(
                "Kunne ikke hente klagebehandling for vedtakId: $vedtakid, feil: {}",
                ex
            )
            null
        }
    }

    companion object {
        const val KLAGE_TABLE: String = "KLAGE"
        private const val VEDTAK_ID = "VEDTAK_ID"
        private const val VEILEDER_IDENT = "VEILEDER_IDENT"
        private const val NORSK_IDENT = "NORSK_IDENT"
        private const val TIDSPUNKT_START_KLAGEBEHANDLING = "TIDSPUNKT_START_KLAGEBEHANDLING"
        private const val KLAGE_DATO = "KLAGE_DATO"
        private const val KLAGE_JOURNALPOST_ID = "KLAGE_JOURNALPOST_ID"
        private const val FORMKRAV_OPPFYLT = "FORMKRAV_OPPFYLT"
        private const val FORMKRAV_BEGRUNNELSE = "FORMKRAV_BEGRUNNELSE"
        private const val RAD_SIST_ENDRET = "RAD_SIST_ENDRET"
        private const val RESULTAT = "RESULTAT"
        private const val RESULTAT_BEGRUNNELSE = "RESULTAT_BEGRUNNELSE"
        private const val TIDSPUNKT_RESULTAT = "TIDSPUNKT_RESULTAT"

    }
}

