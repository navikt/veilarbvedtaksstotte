package no.nav.veilarbvedtaksstotte.repository;

import lombok.SneakyThrows;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import no.nav.veilarbvedtaksstotte.domain.BeslutteroversiktBruker;
import no.nav.veilarbvedtaksstotte.domain.enums.BeslutteroversiktStatus;
import no.nav.veilarbvedtaksstotte.utils.EnumUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.util.List;

import static java.lang.String.format;
import static no.nav.veilarbvedtaksstotte.utils.EnumUtils.getName;

@Repository
public class BeslutteroversiktRepository {

    public final static String BESLUTTEROVERSIKT_BRUKER_TABLE               = "BESLUTTEROVERSIKT_BRUKER";

    private final static String VEDTAK_ID                                   = "VEDTAK_ID";
    private final static String BRUKER_FORNAVN                              = "BRUKER_FORNAVN";
    private final static String BRUKER_ETTERNAVN                            = "BRUKER_ETTERNAVN";
    private final static String BRUKER_OPPFOLGINGSENHET_NAVN                = "BRUKER_OPPFOLGINGSENHET_NAVN";
    private final static String BRUKER_FNR                                  = "BRUKER_FNR";
    private final static String VEDTAK_STARTET                              = "VEDTAK_STARTET";
    private final static String STATUS                                      = "STATUS";
    private final static String BESLUTTER_NAVN                              = "BESLUTTER_NAVN";
    private final static String VEILEDER_NAVN                               = "VEILEDER_NAVN";

    private final JdbcTemplate db;

    @Inject
    public BeslutteroversiktRepository(JdbcTemplate db) {
        this.db = db;
    }

    public void lagBruker(BeslutteroversiktBruker bruker) {
        String sql = format(
            "INSERT INTO %s (%s, %s, %s, %s, %s, %s, %s, %s, %s) VALUES (?, ?, ?, ?, ?, ?, ?::BESLUTTER_OVERSIKT_STATUS_TYPE, ?, ?)",
            BESLUTTEROVERSIKT_BRUKER_TABLE, VEDTAK_ID, BRUKER_FORNAVN, BRUKER_ETTERNAVN,
            BRUKER_OPPFOLGINGSENHET_NAVN, BRUKER_FNR, VEDTAK_STARTET, STATUS, BESLUTTER_NAVN, VEILEDER_NAVN
        );

        db.update(
            sql,
            bruker.getVedtakId(), bruker.getBrukerFornavn(), bruker.getBrukerEtternavn(),
            bruker.getBrukerOppfolgingsenhetNavn(), bruker.getBrukerFnr(), bruker.getVedtakStartet(),
            bruker.getBeslutteroversiktStatus(),  bruker.getBeslutterNavn(), bruker.getVeilederNavn()
        );
    }

    public void oppdaterStatus(long vedtakId, BeslutteroversiktStatus status) {
        String sql = format(
                "UPDATE %s SET %s = ?::BESLUTTER_OVERSIKT_STATUS_TYPE WHERE %s = ?",
                BESLUTTEROVERSIKT_BRUKER_TABLE, STATUS, VEDTAK_ID
        );

        db.update(sql, getName(status), vedtakId);
    }

    public void oppdaterVeileder(long vedtakId, String veilederNavn) {
        String sql = format(
                "UPDATE %s SET %s = ? WHERE %s = ?",
                BESLUTTEROVERSIKT_BRUKER_TABLE, VEILEDER_NAVN, VEDTAK_ID
        );

        db.update(sql, veilederNavn, vedtakId);
    }

    public void oppdaterBeslutter(long vedtakId, String beslutterNavn) {
        String sql = format(
                "UPDATE %s SET %s = ? WHERE %s = ?",
                BESLUTTEROVERSIKT_BRUKER_TABLE, BESLUTTER_NAVN, VEDTAK_ID
        );

        db.update(sql, beslutterNavn, vedtakId);
    }

    public List<BeslutteroversiktBruker> finnBrukere(int limit, int offset) {
        String sql = format(
                "SELECT * FROM %s LIMIT %d OFFSET %d",
                BESLUTTEROVERSIKT_BRUKER_TABLE, limit, offset
        );

        return db.query(sql, new Object[]{}, BeslutteroversiktRepository::mapBeslutteroversiktBruker);
    }

    public BeslutteroversiktBruker finnBrukerForVedtak(long vedtakId) {
        String sql = format(
                "SELECT * FROM %s WHERE %s = ? LIMIT 1",
                BESLUTTEROVERSIKT_BRUKER_TABLE, VEDTAK_ID
        );

        List<BeslutteroversiktBruker> bruker = db.query(sql, new Object[]{vedtakId}, BeslutteroversiktRepository::mapBeslutteroversiktBruker);
        return bruker.isEmpty() ? null : bruker.get(0);
    }

    public void slettBruker(long vedtakId) {
        SqlUtils.delete(db, BESLUTTEROVERSIKT_BRUKER_TABLE)
                .where(WhereClause.equals(VEDTAK_ID, vedtakId))
                .execute();
    }

    @SneakyThrows
    private static BeslutteroversiktBruker mapBeslutteroversiktBruker(ResultSet rs, int rowNum) {
        return new BeslutteroversiktBruker()
                .setVedtakId(rs.getLong(VEDTAK_ID))
                .setBrukerFornavn(rs.getString(BRUKER_FORNAVN))
                .setBrukerEtternavn(rs.getString(BRUKER_ETTERNAVN))
                .setBrukerOppfolgingsenhetNavn(rs.getString(BRUKER_OPPFOLGINGSENHET_NAVN))
                .setBrukerFnr(rs.getString(BRUKER_FNR))
                .setVedtakStartet(rs.getTimestamp(VEDTAK_STARTET).toLocalDateTime())
                .setBeslutteroversiktStatus(EnumUtils.valueOf(BeslutteroversiktStatus.class, rs.getString(STATUS)))
                .setBeslutterNavn(rs.getString(BESLUTTER_NAVN))
                .setVeilederNavn(rs.getString(VEILEDER_NAVN));
    }

}
