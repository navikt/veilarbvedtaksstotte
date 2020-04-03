package no.nav.veilarbvedtaksstotte.repository;

import lombok.SneakyThrows;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import no.nav.veilarbvedtaksstotte.domain.SystemMelding;
import no.nav.veilarbvedtaksstotte.domain.enums.SystemMeldingType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.util.List;

import static no.nav.veilarbvedtaksstotte.utils.EnumUtils.valueOf;

@Repository
public class SystemMeldingRepository {

    public final static String SYSTEM_MELDING_TABLE = "SYSTEM_MELDING";
    private final static String ID                  = "ID";
    private final static String VEDTAK_ID           = "VEDTAK_ID";
    private final static String TYPE                = "MELDING";
    private final static String OPPRETTET           = "OPPRETTET";
    private final static String UTFORT_AV_NAVN      = "UTFORT_AV_NAVN";

    private final JdbcTemplate db;

    @Inject
    public SystemMeldingRepository(JdbcTemplate db) {
        this.db = db;
    }

    public void opprettSystemMelding(long vedtakId, SystemMeldingType systemMeldingType, String utfort_av_navn) {
        SqlUtils.insert(db, SYSTEM_MELDING_TABLE)
                .value(VEDTAK_ID, vedtakId)
                .value(TYPE, systemMeldingType)
                .value(UTFORT_AV_NAVN, utfort_av_navn)
                .execute();
    }

    public List<SystemMelding> hentSystemMeldinger(long vedtakId) {
        return SqlUtils.select(db, SYSTEM_MELDING_TABLE, SystemMeldingRepository::mapSystemMelding)
                .where(WhereClause.equals(VEDTAK_ID, vedtakId))
                .column("*")
                .executeToList();
    }

    @SneakyThrows
    private static SystemMelding mapSystemMelding(ResultSet rs) {
        return new SystemMelding()
                .setId(rs.getInt(ID))
                .setVedtakId(rs.getLong(VEDTAK_ID))
                .setSystemMeldingType(valueOf(SystemMeldingType.class, rs.getString(TYPE)))
                .setOpprettet(rs.getTimestamp(OPPRETTET).toLocalDateTime())
                .setUtfortAvNavn(rs.getString(UTFORT_AV_NAVN));
    }
}
