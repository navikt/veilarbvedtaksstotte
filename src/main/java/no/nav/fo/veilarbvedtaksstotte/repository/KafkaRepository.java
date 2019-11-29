package no.nav.fo.veilarbvedtaksstotte.repository;

import lombok.SneakyThrows;
import no.nav.fo.veilarbvedtaksstotte.domain.KafkaVedtakSendt;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.Innsatsgruppe;
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

    public final static String VEDTAK_SENDT_KAFKA_FEIL_TABLE        = "VEDTAK_SENDT_KAFKA_FEIL";
    private final static String VEDTAK_ID                           = "VEDTAK_ID";
    private final static String VEDTAK_SENDT                        = "VEDTAK_SENDT";
    private final static String INNSATSGRUPPE                       = "INNSATSGRUPPE";
    private final static String AKTOR_ID                            = "AKTOR_ID";
    private final static String ENHET_ID                            = "ENHET_ID";

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

    @SneakyThrows
    private static KafkaVedtakSendt mapKafkaVedtakSendt(ResultSet rs) {
        return new KafkaVedtakSendt()
                .setId(rs.getLong(VEDTAK_ID))
                .setVedtakSendt(rs.getTimestamp(VEDTAK_SENDT).toLocalDateTime())
                .setInnsatsgruppe(valueOf(Innsatsgruppe.class, rs.getString(INNSATSGRUPPE)))
                .setAktorId(rs.getString(AKTOR_ID))
                .setEnhetId(rs.getString(ENHET_ID));
    }

}
