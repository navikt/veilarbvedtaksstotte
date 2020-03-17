package no.nav.veilarbvedtaksstotte.repository;

import lombok.SneakyThrows;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import no.nav.veilarbvedtaksstotte.domain.DialogMelding;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.util.List;

@Repository
public class DialogRepository {

    public final static String DIALOG_MELDING_TABLE = "DIALOG_MELDING";
    private final static String ID                  = "ID";
    private final static String VEDTAK_ID           = "VEDTAK_ID";
    private final static String MELDING             = "MELDING";
    private final static String OPPRETTET           = "OPPRETTET";
    private final static String OPPRETTET_AV_IDENT  = "OPPRETTET_AV_IDENT";

    private final JdbcTemplate db;

    @Inject
    public DialogRepository(JdbcTemplate db) {
        this.db = db;
    }

    public List<DialogMelding> hentDialogMeldinger(long vedtakId) {
        return SqlUtils.select(db, DIALOG_MELDING_TABLE, DialogRepository::mapDialogMelding)
                .where(WhereClause.equals(VEDTAK_ID, vedtakId))
                .column("*")
                .executeToList();
    }

    public void opprettDialogMelding(long vedtakId, String opprettetAvIdent, String melding) {
        SqlUtils.insert(db, DIALOG_MELDING_TABLE)
                .value(VEDTAK_ID, vedtakId)
                .value(OPPRETTET_AV_IDENT, opprettetAvIdent)
                .value(MELDING, melding)
                .execute();
    }

    public boolean slettDialogMeldinger(long vedtakId) {
        return SqlUtils.delete(db, DIALOG_MELDING_TABLE)
                .where(WhereClause.equals(VEDTAK_ID, vedtakId))
                .execute() > 0;
    }

    @SneakyThrows
    private static DialogMelding mapDialogMelding(ResultSet rs) {
        return new DialogMelding()
                .setId(rs.getInt(ID))
                .setVedtakId(rs.getLong(VEDTAK_ID))
                .setMelding(rs.getString(MELDING))
                .setOpprettet(rs.getTimestamp(OPPRETTET).toLocalDateTime())
                .setOpprettetAvIdent(rs.getString(OPPRETTET_AV_IDENT));
    }

}
