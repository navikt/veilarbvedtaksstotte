package no.nav.veilarbvedtaksstotte.repository;

import lombok.SneakyThrows;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import no.nav.veilarbvedtaksstotte.domain.DialogMelding;
import no.nav.veilarbvedtaksstotte.domain.enums.MeldingType;
import no.nav.veilarbvedtaksstotte.domain.enums.MeldingUnderType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.util.List;

import static no.nav.veilarbvedtaksstotte.utils.EnumUtils.getName;
import static no.nav.veilarbvedtaksstotte.utils.EnumUtils.valueOf;

@Repository
public class DialogRepository {

    public final static String DIALOG_MELDING_TABLE = "DIALOG_MELDING";
    private final static String ID                  = "ID";
    private final static String VEDTAK_ID           = "VEDTAK_ID";
    private final static String MELDING             = "MELDING";
    private final static String OPPRETTET           = "OPPRETTET";
    private final static String OPPRETTET_AV_IDENT  = "OPPRETTET_AV_IDENT";
    private final static String MELDING_UNDER_TYPE = "MELDING_UNDER_TYPE";
    private final static String MELDING_TYPE = "MELDING_TYPE";

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

    public void opprettDialogManuellMelding(long vedtakId, String opprettetAvIdent, String melding) {
        db.update(
                "INSERT INTO DIALOG_MELDING(VEDTAK_ID, OPPRETTET_AV_IDENT, MELDING, MELDING_TYPE) VALUES (?, ?, ? ,?::MELDING_TYPE)",
                vedtakId, opprettetAvIdent, melding, getName(MeldingType.MANUELL)
        );
    }

    public void opprettDialogSystemMelding(long vedtakId, String opprettetAvIdent, MeldingUnderType meldingUnderType) {
        db.update(
                "INSERT INTO DIALOG_MELDING(VEDTAK_ID, OPPRETTET_AV_IDENT, MELDING_UNDER_TYPE, MELDING_TYPE) VALUES (?, ?, ?::MELDING_UNDER_TYPE ,?::MELDING_TYPE)",
                vedtakId, opprettetAvIdent, getName(meldingUnderType), getName(MeldingType.SYSTEM)
        );
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
                .setOpprettetAvIdent(rs.getString(OPPRETTET_AV_IDENT))
                .setMeldingUnderType(valueOf(MeldingUnderType.class, rs.getString(MELDING_UNDER_TYPE)))
                .setMeldingType(valueOf(MeldingType.class, rs.getString(MELDING_TYPE)));
    }

}
