package no.nav.veilarbvedtaksstotte.repository;

import lombok.SneakyThrows;
import no.nav.veilarbvedtaksstotte.domain.dialog.DialogMelding;
import no.nav.veilarbvedtaksstotte.domain.dialog.SystemMelding;
import no.nav.veilarbvedtaksstotte.domain.dialog.SystemMeldingType;
import no.nav.veilarbvedtaksstotte.utils.EnumUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.util.List;

import static java.lang.String.format;
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
    private final static String UTFORT_AV_IDENT  = "UTFORT_AV_IDENT";

    private final JdbcTemplate db;

    @Autowired
    public MeldingRepository(JdbcTemplate db) {
        this.db = db;
    }

    public List<DialogMelding> hentDialogMeldinger(long vedtakId) {
        String sql = format("SELECT * FROM %s WHERE %s = %d", DIALOG_MELDING_TABLE, VEDTAK_ID, vedtakId);
        return db.query(sql, MeldingRepository::mapDialogMelding);
    }

    public List<SystemMelding> hentSystemMeldinger(long vedtakId) {
        String sql = format("SELECT * FROM %s WHERE %s = %d", SYSTEM_MELDING_TABLE, VEDTAK_ID, vedtakId);
        return db.query(sql, MeldingRepository::mapSystemMelding);
    }

    public void opprettDialogMelding(long vedtakId, String opprettetAvIdent, String melding) {
        String sql = format("INSERT INTO %s(%s, %s, %s) values(?,?,?)", DIALOG_MELDING_TABLE, vedtakId, opprettetAvIdent, melding);
        db.update(sql, vedtakId, opprettetAvIdent, melding);
    }

    public void opprettSystemMelding(long vedtakId, SystemMeldingType systemMeldingType, String utfortAvIdent) {
        String sql = format(
                "INSERT INTO %s (%s, %s, %s) VALUES (?, ?::SYSTEM_MELDING_TYPE, ?)",
                SYSTEM_MELDING_TABLE, VEDTAK_ID, SYSTEM_MELDING_TYPE, UTFORT_AV_IDENT
        );

        db.update(sql, vedtakId, getName(systemMeldingType), utfortAvIdent);
    }

    public boolean slettMeldinger(long vedtakId) {
        String dialogMeldingSql = format("DELETE FROM %s WHERE %s = %d", DIALOG_MELDING_TABLE, VEDTAK_ID, vedtakId);
        String systemMeldingSql = format("DELETE FROM %s WHERE %s = %d", SYSTEM_MELDING_TABLE, VEDTAK_ID, vedtakId);

        int dialogMeldingerSlettet = db.update(dialogMeldingSql);
        int systemMeldingerSlettet = db.update(systemMeldingSql);

        return (dialogMeldingerSlettet + systemMeldingerSlettet) > 0;
    }

    @SneakyThrows
    private static DialogMelding mapDialogMelding(ResultSet rs, int row) {
        return (DialogMelding) new DialogMelding()
                .setMelding(rs.getString(MELDING))
                .setOpprettetAvIdent(rs.getString(OPPRETTET_AV_IDENT))
                .setOpprettet(rs.getTimestamp(OPPRETTET).toLocalDateTime())
                .setId(rs.getInt(ID))
                .setVedtakId(rs.getLong(VEDTAK_ID));
    }

    @SneakyThrows
    private static SystemMelding mapSystemMelding(ResultSet rs, int row) {
        return (SystemMelding) new SystemMelding()
                .setSystemMeldingType(EnumUtils.valueOf(SystemMeldingType.class, rs.getString(SYSTEM_MELDING_TYPE)))
                .setUtfortAvIdent(rs.getString(UTFORT_AV_IDENT))
                .setOpprettet(rs.getTimestamp(OPPRETTET).toLocalDateTime())
                .setId(rs.getInt(ID))
                .setVedtakId(rs.getLong(VEDTAK_ID));
    }

}
