package no.nav.veilarbvedtaksstotte.repository;

import lombok.SneakyThrows;
import lombok.Value;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import no.nav.veilarbvedtaksstotte.domain.BeslutteroversiktBruker;
import no.nav.veilarbvedtaksstotte.domain.BeslutteroversiktSok;
import no.nav.veilarbvedtaksstotte.domain.BeslutteroversiktSokFilter;
import no.nav.veilarbvedtaksstotte.domain.BrukereMedAntall;
import no.nav.veilarbvedtaksstotte.domain.enums.BeslutteroversiktStatus;
import no.nav.veilarbvedtaksstotte.utils.EnumUtils;
import no.nav.veilarbvedtaksstotte.utils.ValidationUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static no.nav.veilarbvedtaksstotte.utils.EnumUtils.getName;
import static no.nav.veilarbvedtaksstotte.utils.ValidationUtils.isNullOrEmpty;
import static org.apache.commons.lang3.StringUtils.isNumeric;

@Repository
public class BeslutteroversiktRepository {

    public final static String BESLUTTEROVERSIKT_BRUKER_TABLE               = "BESLUTTEROVERSIKT_BRUKER";

    private final static String VEDTAK_ID                                   = "VEDTAK_ID";
    private final static String BRUKER_FORNAVN                              = "BRUKER_FORNAVN";
    private final static String BRUKER_ETTERNAVN                            = "BRUKER_ETTERNAVN";
    private final static String BRUKER_OPPFOLGINGSENHET_NAVN                = "BRUKER_OPPFOLGINGSENHET_NAVN";
    private final static String BRUKER_OPPFOLGINGSENHET_ID                  = "BRUKER_OPPFOLGINGSENHET_ID";
    private final static String BRUKER_FNR                                  = "BRUKER_FNR";
    private final static String VEDTAK_STARTET                              = "VEDTAK_STARTET";
    private final static String STATUS                                      = "STATUS";
    private final static String STATUS_ENDRET                               = "STATUS_ENDRET";
    private final static String BESLUTTER_NAVN                              = "BESLUTTER_NAVN";
    private final static String BESLUTTER_IDENT                             = "BESLUTTER_IDENT";
    private final static String VEILEDER_NAVN                               = "VEILEDER_NAVN";

    private final static Object[] NO_PARAMETERS = new Object[0];
    private final static int DEFAULT_BRUKER_SOK_ANTALL = 20;

    private final JdbcTemplate db;

    @Inject
    public BeslutteroversiktRepository(JdbcTemplate db) {
        this.db = db;
    }

    public void lagBruker(BeslutteroversiktBruker bruker) {
        String sql = format(
            "INSERT INTO %s (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s) VALUES (?, ?, ?, ?, ?, ?, ?, ?::BESLUTTER_OVERSIKT_STATUS_TYPE, ?, ?, ?)",
            BESLUTTEROVERSIKT_BRUKER_TABLE, VEDTAK_ID, BRUKER_FORNAVN, BRUKER_ETTERNAVN,
            BRUKER_OPPFOLGINGSENHET_NAVN, BRUKER_OPPFOLGINGSENHET_ID, BRUKER_FNR,
            VEDTAK_STARTET, STATUS, BESLUTTER_NAVN, BESLUTTER_IDENT, VEILEDER_NAVN
        );

        db.update(
            sql,
            bruker.getVedtakId(), bruker.getBrukerFornavn(), bruker.getBrukerEtternavn(),
            bruker.getBrukerOppfolgingsenhetNavn(), bruker.getBrukerOppfolgingsenhetId(),
            bruker.getBrukerFnr(), bruker.getVedtakStartet(), getName(bruker.getStatus()),
            bruker.getBeslutterNavn(), bruker.getBeslutterIdent(), bruker.getVeilederNavn()
        );
    }

    public void oppdaterStatus(long vedtakId, BeslutteroversiktStatus status) {
        String sql = format(
                "UPDATE %s SET %s = ?::BESLUTTER_OVERSIKT_STATUS_TYPE, %s = CURRENT_TIMESTAMP WHERE %s = ?",
                BESLUTTEROVERSIKT_BRUKER_TABLE, STATUS, STATUS_ENDRET, VEDTAK_ID
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

    public void oppdaterBeslutter(long vedtakId, String beslutterIdent, String beslutterNavn) {
        String sql = format(
                "UPDATE %s SET %s = ?, %s = ? WHERE %s = ?",
                BESLUTTEROVERSIKT_BRUKER_TABLE, BESLUTTER_NAVN, BESLUTTER_IDENT, VEDTAK_ID
        );

        db.update(sql, beslutterNavn, beslutterIdent, vedtakId);
    }

    public BrukereMedAntall sokEtterBrukere(BeslutteroversiktSok sok, String innloggetVeilederIdent) {
        Object[] parameters = NO_PARAMETERS;
        StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM ").append(BESLUTTEROVERSIKT_BRUKER_TABLE);

        Optional<SqlWithParameters> maybeFilterSqlWithParams = createFilterSqlWithParameters(sok.getFilter(), innloggetVeilederIdent);
        Optional<String> maybeOrderBySql = lagOrderBySql(sok.getOrderByField(), sok.getOrderByDirection());

        if (maybeFilterSqlWithParams.isPresent()) {
            SqlWithParameters filterSqlWithParams = maybeFilterSqlWithParams.get();
            parameters = filterSqlWithParams.parameters;
            sqlBuilder.append(" ").append(filterSqlWithParams.sql);
        }

        maybeOrderBySql.ifPresent(orderBySql -> sqlBuilder.append(" ").append(orderBySql));

        sqlBuilder.append(" ").append(lagPaginationSql(sok.getAntall(), sok.getFra()));

        long totaltAntallSokteBrukere = totaltAntallBrukereForSok(sok, innloggetVeilederIdent);
        List<BeslutteroversiktBruker> brukere = db.query(sqlBuilder.toString(), parameters, BeslutteroversiktRepository::mapBeslutteroversiktBruker);

        return new BrukereMedAntall(brukere, totaltAntallSokteBrukere);
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

    private long totaltAntallBrukereForSok(BeslutteroversiktSok sok, String innloggetVeilederIdent) {
        Object[] parameters = NO_PARAMETERS;
        StringBuilder sqlBuilder = new StringBuilder("SELECT count(*) FROM ").append(BESLUTTEROVERSIKT_BRUKER_TABLE);

        Optional<SqlWithParameters> maybeFilterSqlWithParams = createFilterSqlWithParameters(sok.getFilter(), innloggetVeilederIdent);

        if (maybeFilterSqlWithParams.isPresent()) {
            SqlWithParameters filterSqlWithParams = maybeFilterSqlWithParams.get();
            parameters = filterSqlWithParams.parameters;
            sqlBuilder.append(" ").append(filterSqlWithParams.sql);
        }

        Long count = db.queryForObject(sqlBuilder.toString(), parameters, BeslutteroversiktRepository::mapCount);
        return count == null ? 0 : count;
    }

    private boolean harAktivtFilter(BeslutteroversiktSokFilter filter) {
        return filter != null
                && (!isNullOrEmpty(filter.getEnheter())
                || filter.getStatus() != null
                || filter.isVisMineBrukere()
                || !isNullOrEmpty(filter.getNavnEllerFnr()));
    }

    private Optional<String> lagOrderBySql(BeslutteroversiktSok.OrderByField field, BeslutteroversiktSok.OrderByDirection direction) {
        if (field == null || direction == null) {
            return Optional.empty();
        }

        return Optional.of(format("ORDER BY %s %s", getName(field), getName(direction)));
    }

    private String lagPaginationSql(int antall, int fra) {
        antall = antall <= 0 ? DEFAULT_BRUKER_SOK_ANTALL : antall;
        fra = Math.max(0, fra);
        return format("LIMIT %d OFFSET %d", antall, fra);
    }

    private Optional<SqlWithParameters> createFilterSqlWithParameters(BeslutteroversiktSokFilter filter, String innloggetVeilederIdent) {
        if (!harAktivtFilter(filter)) {
            return Optional.empty();
        }

        List<String> filterStrs = new ArrayList<>();
        List<Object> parameters = new ArrayList<>();

        if (!isNullOrEmpty(filter.getEnheter())) {
            filterStrs.add(format("%s = SOME(?::varchar[])", BRUKER_OPPFOLGINGSENHET_ID));
            parameters.add(toPostgresArray(filter.getEnheter()));
        }

        if (!ValidationUtils.isNullOrEmpty(filter.getNavnEllerFnr())) {
            String nameOrFnrCol = isNumeric(filter.getNavnEllerFnr()) ? BRUKER_FNR : BRUKER_ETTERNAVN; // TODO: Bedre søk på navn
            filterStrs.add(format("%s ILIKE ?", nameOrFnrCol));
            parameters.add("%" + filter.getNavnEllerFnr() + "%"); // TODO: Check if this is the correct way
        }

        if (filter.isVisMineBrukere()) {
            filterStrs.add(format("%s = ?", BESLUTTER_IDENT));
            parameters.add(innloggetVeilederIdent);
        }

        if (filter.getStatus() != null) {
            filterStrs.add(format("%s = ?::BESLUTTER_OVERSIKT_STATUS_TYPE", STATUS));
            parameters.add(getName(filter.getStatus()));
        }

        String sqlStr = "WHERE " + String.join(" AND ", filterStrs);
        return Optional.of(new SqlWithParameters(sqlStr, parameters.toArray()));
    }

    private String toPostgresArray(List<String> values) {
        return "{" + String.join(",", values) + "}";
    }

    @Value
    public class SqlWithParameters {
        String sql;
        Object[] parameters;
    }

    @SneakyThrows
    private static long mapCount(ResultSet rs, int rowNum) {
        return rs.getLong(1);
    }

    @SneakyThrows
    private static BeslutteroversiktBruker mapBeslutteroversiktBruker(ResultSet rs, int rowNum) {
        return new BeslutteroversiktBruker()
                .setVedtakId(rs.getLong(VEDTAK_ID))
                .setBrukerFornavn(rs.getString(BRUKER_FORNAVN))
                .setBrukerEtternavn(rs.getString(BRUKER_ETTERNAVN))
                .setBrukerOppfolgingsenhetNavn(rs.getString(BRUKER_OPPFOLGINGSENHET_NAVN))
                .setBrukerOppfolgingsenhetId(rs.getString(BRUKER_OPPFOLGINGSENHET_ID))
                .setBrukerFnr(rs.getString(BRUKER_FNR))
                .setVedtakStartet(rs.getTimestamp(VEDTAK_STARTET).toLocalDateTime())
                .setStatus(EnumUtils.valueOf(BeslutteroversiktStatus.class, rs.getString(STATUS)))
                .setStatusEndret(rs.getTimestamp(STATUS_ENDRET).toLocalDateTime())
                .setBeslutterNavn(rs.getString(BESLUTTER_NAVN))
                .setBeslutterIdent(rs.getString(BESLUTTER_IDENT))
                .setVeilederNavn(rs.getString(VEILEDER_NAVN));
    }

}
