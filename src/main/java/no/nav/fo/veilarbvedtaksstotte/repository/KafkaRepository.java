package no.nav.fo.veilarbvedtaksstotte.repository;

import lombok.SneakyThrows;
import no.nav.fo.veilarbvedtaksstotte.domain.KafkaVedtakSendt;
import no.nav.fo.veilarbvedtaksstotte.domain.KafkaVedtakStatusEndring;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.Hovedmal;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.Innsatsgruppe;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.KafkaVedtakStatus;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.util.List;

import static no.nav.fo.veilarbvedtaksstotte.utils.EnumUtils.getName;
import static no.nav.fo.veilarbvedtaksstotte.utils.EnumUtils.valueOf;

@Repository
public class KafkaRepository {

    public final static String VEDTAK_SENDT_KAFKA_FEIL_TABLE            = "VEDTAK_SENDT_KAFKA_FEIL";
    public final static String VEDTAK_STATUS_ENDRING_KAFKA_FEIL_TABLE   = "VEDTAK_STATUS_ENDRING_KAFKA_FEIL";

    private final static String ID                                      = "ID";
    private final static String VEDTAK_ID                               = "VEDTAK_ID";
    private final static String VEDTAK_SENDT                            = "VEDTAK_SENDT";
    private final static String INNSATSGRUPPE                           = "INNSATSGRUPPE";
    private final static String AKTOR_ID                                = "AKTOR_ID";
    private final static String ENHET_ID                                = "ENHET_ID";
    private final static String HOVEDMAL                                = "HOVEDMAL";
    private final static String SIST_REDIGERT_TIDSPUNKT                 = "SIST_REDIGERT_TIDSPUNKT";
    private final static String STATUS_ENDRET_TIDSPUNKT                 = "STATUS_ENDRET_TIDSPUNKT";
    private final static String KAFKA_VEDTAK_STATUS                     = "KAFKA_VEDTAK_STATUS";

    private final JdbcTemplate db;

    @Inject
    public KafkaRepository(JdbcTemplate db) {
        this.db = db;
    }

    public void lagreVedtakSendtKafkaFeil(KafkaVedtakSendt vedtakSendt) {
        SqlUtils.insert(db, VEDTAK_SENDT_KAFKA_FEIL_TABLE)
                .value(VEDTAK_ID, vedtakSendt.getId())
                .value(INNSATSGRUPPE, getName(vedtakSendt.getInnsatsgruppe()))
                .value(AKTOR_ID, vedtakSendt.getAktorId())
                .value(VEDTAK_SENDT, vedtakSendt.getVedtakSendt())
                .value(ENHET_ID, vedtakSendt.getEnhetId())
                .execute();
    }

    public List<KafkaVedtakSendt> hentFeiledeVedtakSendt() {
        return SqlUtils.select(db, VEDTAK_SENDT_KAFKA_FEIL_TABLE, KafkaRepository::mapKafkaVedtakSendt)
                .column("*")
                .executeToList();
    }

    public void slettVedtakSendtKafkaFeil(String aktorId) {
        SqlUtils.delete(db, VEDTAK_SENDT_KAFKA_FEIL_TABLE)
                .where(WhereClause.equals(AKTOR_ID, aktorId))
                .execute();
    }


    public void lagreVedtakStatusEndringKafkaFeil(KafkaVedtakStatusEndring vedtakStatusEndring) {
        String sql = String.format(
            "INSERT INTO %s (%s, %s, %s, %s, %s, %s) VALUES (?::KAFKA_VEDTAK_STATUS_TYPE,?,?,?,?,?)",
                VEDTAK_STATUS_ENDRING_KAFKA_FEIL_TABLE, KAFKA_VEDTAK_STATUS, AKTOR_ID, HOVEDMAL,
                INNSATSGRUPPE, SIST_REDIGERT_TIDSPUNKT, STATUS_ENDRET_TIDSPUNKT
        );

        db.update(
                sql, getName(vedtakStatusEndring.getVedtakStatus()), vedtakStatusEndring.getAktorId(),
                getName(vedtakStatusEndring.getHovedmal()), getName(vedtakStatusEndring.getInnsatsgruppe()),
                vedtakStatusEndring.getSistRedigertTidspunkt(), vedtakStatusEndring.getStatusEndretTidspunkt()
        );
    }

    public List<KafkaVedtakStatusEndring> hentFeiledeVedtakStatusEndringer() {
        return SqlUtils.select(db, VEDTAK_STATUS_ENDRING_KAFKA_FEIL_TABLE, KafkaRepository::mapKafkaVedtakStatusEndring)
                .column("*")
                .executeToList();
    }

    public void slettVedtakStatusEndringKafkaFeil(long id, KafkaVedtakStatus vedtakStatus) {
        String sql = String.format(
                "DELETE FROM %s WHERE %s = ? AND %s = ?::KAFKA_VEDTAK_STATUS_TYPE",
                VEDTAK_STATUS_ENDRING_KAFKA_FEIL_TABLE, ID, KAFKA_VEDTAK_STATUS
        );

        db.update(sql, id, getName(vedtakStatus));
    }


    @SneakyThrows
    private static KafkaVedtakSendt mapKafkaVedtakSendt(ResultSet rs) {
        return new KafkaVedtakSendt()
                .setId(rs.getLong(VEDTAK_ID))
                .setVedtakSendt(rs.getTimestamp(VEDTAK_SENDT).toLocalDateTime())
                .setInnsatsgruppe(valueOf(Innsatsgruppe.class, rs.getString(INNSATSGRUPPE)))
                .setAktorId(rs.getString(AKTOR_ID))
                .setEnhetId(rs.getString(ENHET_ID));
    }

    @SneakyThrows
    private static KafkaVedtakStatusEndring mapKafkaVedtakStatusEndring(ResultSet rs) {
        return new KafkaVedtakStatusEndring()
                .setId(rs.getLong(ID))
                .setAktorId(rs.getString(AKTOR_ID))
                .setHovedmal(valueOf(Hovedmal.class, rs.getString(HOVEDMAL)))
                .setInnsatsgruppe(valueOf(Innsatsgruppe.class, rs.getString(INNSATSGRUPPE)))
                .setSistRedigertTidspunkt(rs.getTimestamp(SIST_REDIGERT_TIDSPUNKT).toLocalDateTime())
                .setStatusEndretTidspunkt(rs.getTimestamp(STATUS_ENDRET_TIDSPUNKT).toLocalDateTime())
                .setVedtakStatus(valueOf(KafkaVedtakStatus.class, rs.getString(KAFKA_VEDTAK_STATUS)));
    }

}
