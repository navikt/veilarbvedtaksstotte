package no.nav.veilarbvedtaksstotte.repository;

import lombok.SneakyThrows;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import no.nav.veilarbvedtaksstotte.domain.dialog.DialogMelding;
import no.nav.veilarbvedtaksstotte.domain.dialog.SystemMelding;
import no.nav.veilarbvedtaksstotte.domain.dialog.SystemMeldingType;
import no.nav.veilarbvedtaksstotte.utils.EnumUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.util.List;

import static no.nav.veilarbvedtaksstotte.utils.EnumUtils.getName;

@Repository
public class MeldingRepository {

    public final static String DIALOG_MELDING_TABLE = "DIALOG_MELDING";
    public final static String SYSTEM_MELDING_TABLE = "SYSTEM_MELDING";

    private final static String ID                  = "ID";
    private final static String VEDTAK_ID           = "VEDTAK_ID";
    private final static String OPPRETTET           = "OPPRETTET";

    // Dialog melding
    private final static String MELDING             = "MELDING";
    private final static String OPPRETTET_AV_IDENT  = "OPPRETTET_AV_IDENT";

    // System melding
    private final static String SYSTEM_MELDING_TYPE = "SYSTEM_MELDING_TYPE";
    private final static String UTFORT_AV_NAVN  = "UTFORT_AV_NAVN";

    private final JdbcTemplate db;

    @Inject
    public MeldingRepository(JdbcTemplate db) {
        this.db = db;
    }

    public List<DialogMelding> hentDialogMeldinger(long vedtakId) {
        return SqlUtils.select(db, DIALOG_MELDING_TABLE, MeldingRepository::mapDialogMelding)
                .where(WhereClause.equals(VEDTAK_ID, vedtakId))
                .column("*")
                .executeToList();
    }

    public List<SystemMelding> hentSystemMeldinger(long vedtakId) {
        return SqlUtils.select(db, SYSTEM_MELDING_TABLE, MeldingRepository::mapSystemMelding)
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

    public void opprettSystemMelding(long vedtakId, SystemMeldingType systemMeldingType, String utfortAvNavn) {
        String sql = String.format(
                "INSERT INTO %s (%s, %s, %s) VALUES (?, ?::SYSTEM_MELDING_TYPE, ?)",
                SYSTEM_MELDING_TABLE, VEDTAK_ID, SYSTEM_MELDING_TYPE, UTFORT_AV_NAVN
        );

        db.update(sql, vedtakId, getName(systemMeldingType), utfortAvNavn);
    }

    public boolean slettMeldinger(long vedtakId) {
        int dialogMeldingerSlettet = SqlUtils.delete(db, DIALOG_MELDING_TABLE)
                .where(WhereClause.equals(VEDTAK_ID, vedtakId))
                .execute();

        int systemMeldingerSlettet = SqlUtils.delete(db, SYSTEM_MELDING_TABLE)
                .where(WhereClause.equals(VEDTAK_ID, vedtakId))
                .execute();

        return (dialogMeldingerSlettet + systemMeldingerSlettet) > 0;
    }

    @SneakyThrows
    private static DialogMelding mapDialogMelding(ResultSet rs) {
        return (DialogMelding) new DialogMelding()
                .setMelding(rs.getString(MELDING))
                .setOpprettetAvIdent(rs.getString(OPPRETTET_AV_IDENT))
                .setOpprettet(rs.getTimestamp(OPPRETTET).toLocalDateTime())
                .setId(rs.getInt(ID))
                .setVedtakId(rs.getLong(VEDTAK_ID));
    }

    @SneakyThrows
    private static SystemMelding mapSystemMelding(ResultSet rs) {
        return (SystemMelding) new SystemMelding()
                .setSystemMeldingType(EnumUtils.valueOf(SystemMeldingType.class, rs.getString(SYSTEM_MELDING_TYPE)))
                .setUtfortAvNavn(rs.getString(UTFORT_AV_NAVN))
                .setOpprettet(rs.getTimestamp(OPPRETTET).toLocalDateTime())
                .setId(rs.getInt(ID))
                .setVedtakId(rs.getLong(VEDTAK_ID));
    }

}
