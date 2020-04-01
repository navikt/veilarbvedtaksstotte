package no.nav.veilarbvedtaksstotte.repository;

import lombok.SneakyThrows;
import lombok.Value;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import no.nav.veilarbvedtaksstotte.domain.BeslutteroversiktBruker;
import no.nav.veilarbvedtaksstotte.domain.BeslutteroversiktSok;
import no.nav.veilarbvedtaksstotte.domain.BeslutteroversiktSokFilter;
import no.nav.veilarbvedtaksstotte.domain.enums.BeslutteroversiktStatus;
import no.nav.veilarbvedtaksstotte.utils.EnumUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static no.nav.veilarbvedtaksstotte.utils.EnumUtils.getName;
import static no.nav.veilarbvedtaksstotte.utils.ValidationUtils.isListEmpty;
import static no.nav.veilarbvedtaksstotte.utils.ValidationUtils.isStringBlank;
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
    private final static String BESLUTTER_NAVN                              = "BESLUTTER_NAVN";
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
            "INSERT INTO %s (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s) VALUES (?, ?, ?, ?, ?, ?, ?, ?::BESLUTTER_OVERSIKT_STATUS_TYPE, ?, ?)",
            BESLUTTEROVERSIKT_BRUKER_TABLE, VEDTAK_ID, BRUKER_FORNAVN, BRUKER_ETTERNAVN,
            BRUKER_OPPFOLGINGSENHET_NAVN, BRUKER_OPPFOLGINGSENHET_ID, BRUKER_FNR, VEDTAK_STARTET, STATUS, BESLUTTER_NAVN, VEILEDER_NAVN
        );

        db.update(
            sql,
            bruker.getVedtakId(), bruker.getBrukerFornavn(), bruker.getBrukerEtternavn(),
            bruker.getBrukerOppfolgingsenhetNavn(), bruker.getBrukerOppfolgingsenhetId(),
            bruker.getBrukerFnr(), bruker.getVedtakStartet(), getName(bruker.getStatus()),
            bruker.getBeslutterNavn(), bruker.getVeilederNavn()
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

    public List<BeslutteroversiktBruker> sokEtterBrukere(BeslutteroversiktSok sok) {
        Object[] parameters = NO_PARAMETERS;
        StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM ").append(BESLUTTEROVERSIKT_BRUKER_TABLE);

        Optional<SqlWithParameters> maybeFilterSqlWithParams = createFilterSqlWithParameters(sok.getFilter());
        Optional<String> maybeOrderBySql = lagOrderBySql(sok.getOrderByField(), sok.getOrderByDirection());

        if (maybeFilterSqlWithParams.isPresent()) {
            SqlWithParameters filterSqlWithParams = maybeFilterSqlWithParams.get();
            parameters = filterSqlWithParams.parameters;
            sqlBuilder.append(" ").append(filterSqlWithParams.sql);
        }

        maybeOrderBySql.ifPresent(orderBySql -> sqlBuilder.append(" ").append(orderBySql));

        sqlBuilder.append(" ").append(lagPaginationSql(sok.getAntall(), sok.getFra()));

        return db.query(sqlBuilder.toString(), parameters, BeslutteroversiktRepository::mapBeslutteroversiktBruker);
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

    private boolean harAktivtFilter(BeslutteroversiktSokFilter filter) {
        return filter != null
                && (isListEmpty(filter.getEnheter())
                || filter.getStatus() != null
                || filter.getBrukerFilter() != null
                || isStringBlank(filter.getNavnEllerFnr()));
    }

    private Optional<String> lagOrderBySql(BeslutteroversiktSok.OrderByField field, BeslutteroversiktSok.OrderByDirection direction) {
        if (field == null || direction == null) {
            return Optional.empty();
        }

        return Optional.of(format("ORDER BY %s %s", mapOrderByFieldToColumnName(field), getName(direction)));
    }

    private String lagPaginationSql(int antall, int fra) {
        antall = antall >= 0 ? DEFAULT_BRUKER_SOK_ANTALL : antall;
        fra = Math.max(0, fra);
        return format("LIMIT %d OFFSET %d", antall, fra);
    }

    private String mapOrderByFieldToColumnName(BeslutteroversiktSok.OrderByField field) {
        switch (field) {
            case BRUKER_ETTERNAVN:
                return BRUKER_ETTERNAVN;
            case BRUKER_OPPFOLGINGSENHET_NAVN:
                return BRUKER_OPPFOLGINGSENHET_NAVN;
            case BRUKER_FNR:
                return BRUKER_FNR;
            case VEDTAK_STARTET:
                return VEDTAK_STARTET;
            case STATUS:
                return STATUS;
            case BESLUTTER_NAVN:
                return BESLUTTER_NAVN;
            case VEILEDER_NAVN:
                return VEILEDER_NAVN;
            default:
                throw new IllegalArgumentException("Unknown field " + getName(field));
        }
    }

    private Optional<SqlWithParameters> createFilterSqlWithParameters(BeslutteroversiktSokFilter filter) {
        if (!harAktivtFilter(filter)) {
            return Optional.empty();
        }

        StringBuilder sqlBuilder = new StringBuilder("WHERE");
        List<Object> parameters = new ArrayList<>();

        if (!isListEmpty(filter.getEnheter())) {
            sqlBuilder.append(format(" %s = SOME(?::varchar[])", BRUKER_OPPFOLGINGSENHET_ID));
            System.out.println(toPostgresArray(filter.getEnheter()));
            parameters.add(toPostgresArray(filter.getEnheter()));
        }

        if (!isStringBlank(filter.getNavnEllerFnr())) {
            String nameOrFnrCol = isNumeric(filter.getNavnEllerFnr()) ? BRUKER_FNR : BRUKER_ETTERNAVN;
            sqlBuilder.append(format(" %s ILIKE ?", nameOrFnrCol));
            parameters.add("%" + filter.getNavnEllerFnr() + "%"); // TODO: Check if this is the correct way
        }

        if (filter.getStatus() != null) {
            sqlBuilder.append(format(" %s = ?::BESLUTTER_OVERSIKT_STATUS_TYPE", STATUS));
            parameters.add(getName(filter.getStatus()));
        }

        return Optional.of(new SqlWithParameters(sqlBuilder.toString(), parameters.toArray()));
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
                .setBeslutterNavn(rs.getString(BESLUTTER_NAVN))
                .setVeilederNavn(rs.getString(VEILEDER_NAVN));
    }

}
