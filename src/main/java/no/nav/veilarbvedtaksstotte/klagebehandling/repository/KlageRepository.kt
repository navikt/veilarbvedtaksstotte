package no.nav.veilarbvedtaksstotte.klagebehandling.repository

import no.nav.veilarbvedtaksstotte.klagebehandling.controller.FormkravKlagefristUnntakSvar
import no.nav.veilarbvedtaksstotte.klagebehandling.controller.FormkravSvar
import no.nav.veilarbvedtaksstotte.klagebehandling.domene.*
import no.nav.veilarbvedtaksstotte.utils.SecureLog.secureLog
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class KlageRepository(private val db: JdbcTemplate) {

    fun upsertKlagebehandling(
        klagebehandling: KlageBehandling
    ) {
        val (generellData) = klagebehandling;

        val sql = """
            INSERT INTO $KLAGE_TABLE (
                $VEDTAK_ID, 
                $VEILEDER_IDENT, 
                $NORSK_IDENT, 
                $KLAGE_DATO,
                $KLAGE_JOURNALPOST_ID,
                $TIDSPUNKT_START_KLAGEBEHANDLING, 
                $RAD_SIST_ENDRET, 
                $FORMKRAV_OPPFYLT, 
                $RESULTAT ,
                $STATUS
            )
            VALUES (?,?,?,?,?,current_timestamp, current_timestamp, ?, ?, ?)
            ON CONFLICT ($VEDTAK_ID) 
            DO UPDATE SET 
            $VEILEDER_IDENT = EXCLUDED.${VEILEDER_IDENT},
            $NORSK_IDENT = EXCLUDED.${NORSK_IDENT},
            $KLAGE_DATO = EXCLUDED.${KLAGE_DATO},
            $KLAGE_JOURNALPOST_ID = EXCLUDED.${KLAGE_JOURNALPOST_ID},
            RAD_SIST_ENDRET = current_timestamp 
        """.trimIndent()
        try {
            db.update(
                sql,
                generellData.vedtakId,
                generellData.veilederIdent,
                generellData.norskIdent,
                generellData.klageDato,
                generellData.klageJournalpostid,
                FormkravOppfylt.IKKE_SATT.name,
                Resultat.IKKE_SATT.name,
                Status.UTKAST.name
            )
        } catch (ex: Exception) {
            secureLog.error(
                "Kunne ikke lagre klagebehandling for vedtakId: ${generellData.vedtakId}, feil: {}",
                ex
            )
        }
    }

    fun updateFormkrav(
        vedtakId: Long,
        formkrav: FormkravData,
        formkravOppfylt: FormkravOppfylt,
    ) {
        val sql = """
                UPDATE $KLAGE_TABLE SET
                    $FORMKRAV_SIGNERT = ?,
                    $FORMKRAV_PART = ?,
                    $FORMKRAV_KONKRET = ?,
                    $FORMKRAV_KLAGEFRIST_OPPRETTHOLDT = ?,
                    $FORMKRAV_KLAGEFRIST_UNNTAK = ?,
                    $FORMKRAV_OPPFYLT = ?,
                    $FORMKRAV_BEGRUNNELSE_INTERN = ?,
                    $FORMKRAV_BEGRUNNELSE_BREV = ?,
                    $TIDSPUNKT_FORMKRAV = current_timestamp,
                    $RAD_SIST_ENDRET = current_timestamp
                WHERE $VEDTAK_ID = ?
            """.trimIndent()
        try {
            db.update(
                sql,
                formkrav.formkravSignert?.name,
                formkrav.formkravPart?.name,
                formkrav.formkravKonkret?.name,
                formkrav.formkravKlagefristOpprettholdt?.name,
                formkrav.formkravKlagefristUnntak?.name,
                formkravOppfylt.name,
                formkrav.formkravBegrunnelseIntern,
                formkrav.formkravBegrunnelseBrev,
                vedtakId
            )
        } catch (ex: Exception) {
            secureLog.error(
                "Kunne ikke lagre formkrav for klagebehandling for vedtakId: ${vedtakId}, feil: {}",
                ex
            )
        }

    }

    fun updateResultat(
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

    fun updateStatus(
        vedtakid: Long,
        status: Status
    ) {
        val sql = """
                UPDATE $KLAGE_TABLE SET
                    $STATUS = ?,
                    $TIDSPUNKT_OVERSENDT_TIL_KABAL = current_timestamp,
                    $RAD_SIST_ENDRET = current_timestamp
                WHERE $VEDTAK_ID = ?
            """.trimIndent()
        try {
            db.update(sql, status.toString(), vedtakid)
        } catch (ex: Exception) {
            secureLog.error(
                "Kunne ikke oppdatere status for klagebehandling for vedtakId: $vedtakid, feil: {}",
                ex
            )
        }
    }

    fun hentKlageBehandling(vedtakid: Long): KlageBehandling? {
        val sql = "SELECT * FROM $KLAGE_TABLE WHERE $VEDTAK_ID = ?"
        return try {
            db.queryForObject(sql, { rs, _ ->
                KlageBehandling(
                    generellData = GenerellData(
                        vedtakId = rs.getLong(VEDTAK_ID),
                        veilederIdent = rs.getString(VEILEDER_IDENT),
                        norskIdent = rs.getString(NORSK_IDENT),
                        klageDato = rs.getDate(KLAGE_DATO).toLocalDate(),
                        klageJournalpostid = rs.getString(KLAGE_JOURNALPOST_ID),
                    ),

                    formkravData = FormkravData(
                        formkravSignert = rs.getString(FORMKRAV_SIGNERT)?.let { FormkravSvar.valueOf(it) },
                        formkravPart = rs.getString(FORMKRAV_PART)?.let { FormkravSvar.valueOf(it) },
                        formkravKonkret = rs.getString(FORMKRAV_KONKRET)?.let { FormkravSvar.valueOf(it) },
                        formkravKlagefristOpprettholdt = rs.getString(FORMKRAV_KLAGEFRIST_OPPRETTHOLDT)
                            ?.let { FormkravSvar.valueOf(it) },
                        formkravKlagefristUnntak = rs.getString(FORMKRAV_KLAGEFRIST_UNNTAK)
                            ?.let { FormkravKlagefristUnntakSvar.valueOf(it) },
                        formkravOppfylt = rs.getString(FORMKRAV_OPPFYLT).let { FormkravOppfylt.valueOf(it) },
                        formkravBegrunnelseIntern = rs.getString(FORMKRAV_BEGRUNNELSE_INTERN),
                        formkravBegrunnelseBrev = rs.getString(FORMKRAV_BEGRUNNELSE_BREV),
                    ),

                    resultatData = ResultatData(
                        resultat = rs.getString(RESULTAT).let { Resultat.valueOf(it) },
                        resultatBegrunnelse = rs.getString(RESULTAT_BEGRUNNELSE),
                        status = rs.getString(STATUS)?.let { Status.valueOf(it) } ?: Status.UTKAST
                    ),
                )
            }, vedtakid)
        } catch (_: EmptyResultDataAccessException) {
            null
        } catch (ex: Exception) {
            secureLog.error(
                "Kunne ikke hente klagebehandling for vedtakId: $vedtakid, feil: {}",
                ex
            )
            throw ex
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

        private const val FORMKRAV_SIGNERT = "FORMKRAV_SIGNERT"
        private const val FORMKRAV_PART = "FORMKRAV_PART"
        private const val FORMKRAV_KONKRET = "FORMKRAV_KONKRET"
        private const val FORMKRAV_KLAGEFRIST_OPPRETTHOLDT = "FORMKRAV_KLAGEFRIST_OPPRETTHOLDT"
        private const val FORMKRAV_KLAGEFRIST_UNNTAK = "FORMKRAV_KLAGEFRIST_UNNTAK"
        private const val FORMKRAV_OPPFYLT = "FORMKRAV_OPPFYLT"
        private const val FORMKRAV_BEGRUNNELSE_INTERN = "FORMKRAV_BEGRUNNELSE_INTERN"
        private const val FORMKRAV_BEGRUNNELSE_BREV = "FORMKRAV_BEGRUNNELSE_BREV"
        private const val TIDSPUNKT_FORMKRAV = "TIDSPUNKT_FORMKRAV"
        private const val BREV_FORMKRAV_AVVIST_JOURNALPOST_ID = "BREV_FORMKRAV_AVVIST_JOURNALPOST_ID"

        private const val RESULTAT = "RESULTAT"
        private const val RESULTAT_BEGRUNNELSE = "RESULTAT_BEGRUNNELSE"
        private const val TIDSPUNKT_RESULTAT = "TIDSPUNKT_RESULTAT"
        private const val TIDSPUNKT_OVERSENDT_TIL_KABAL = "TIDSPUNKT_OVERSENDT_TIL_KABAL"

        private const val STATUS = "STATUS"
        private const val RAD_SIST_ENDRET = "RAD_SIST_ENDRET"

    }
}

