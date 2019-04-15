package no.nav.fo.veilarbvedtaksstotte.repository;

import lombok.SneakyThrows;
import no.nav.fo.veilarbvedtaksstotte.domain.Vedtak;
import no.nav.fo.veilarbvedtaksstotte.domain.Veileder;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.Hovedmal;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.Innsatsgruppe;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.VedtakStatus;
import no.nav.sbl.sql.DbConstants;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.order.OrderClause;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.inject.Inject;
import java.sql.ResultSet;

import static no.nav.fo.veilarbvedtaksstotte.utils.DbUtils.nesteFraSekvens;
import static no.nav.fo.veilarbvedtaksstotte.utils.EnumUtils.getName;
import static no.nav.fo.veilarbvedtaksstotte.utils.EnumUtils.valueOf;

@Repository
public class VedtaksstotteRepository {

    private final static long NO_ID =  -1;

    private final static String VEDTAK              = "VEDTAK";
    private final static String VEDTAK_SEQ          = "VEDTAK_SEQ";
    private final static String VEDTAK_ID           = "VEDTAK_ID";
    private final static String AKTOR_ID            = "AKTOR_ID";
    private final static String HOVEDMAL            = "HOVEDMAL";
    private final static String INNSATSGRUPPE       = "INNSATSGRUPPE";
    private final static String VEILEDER_IDENT      = "VEILEDER_IDENT";
    private final static String VEILEDER_ENHET_ID   = "VEILEDER_ENHET_ID";
    private final static String SIST_OPPDATERT      = "SIST_OPPDATERT";
    private final static String SENDT               = "SENDT";
    private final static String BEGRUNNELSE         = "BEGRUNNELSE";
    private final static String STATUS              = "STATUS";

    private final JdbcTemplate db;

    @Inject
    public VedtaksstotteRepository(JdbcTemplate db) {
        this.db = db;
    }


    public void markerVedtakSomSendt(long vedtakId){
        SqlUtils.update(db, VEDTAK)
                .whereEquals(VEDTAK_ID, vedtakId)
                .set(SIST_OPPDATERT, DbConstants.CURRENT_TIMESTAMP)
                .set(SENDT, DbConstants.CURRENT_TIMESTAMP)
                .set(STATUS, getName(VedtakStatus.SENDT))
                .execute();
    }

    public void upsertUtkast(String aktorId, Vedtak vedtak) {
        long id = hentVedtakUtkastId(aktorId);

        if (id != NO_ID) {
            oppdaterVedtakUtkast(id, vedtak);
        } else {
            lagVedtakUtkast(aktorId, vedtak);
        }
    }

    public Vedtak hentVedtak(String aktorId, boolean kunUtkast) {
        WhereClause where = WhereClause.equals(AKTOR_ID, aktorId);

        if (kunUtkast) {
            where = where.and(WhereClause.equals(STATUS, getName(VedtakStatus.UTKAST)));
        }

        return SqlUtils.select(db, VEDTAK, VedtaksstotteRepository::mapVedtakUtkast)
                .where(where)
                .orderBy(OrderClause.desc(SIST_OPPDATERT))
                .limit(1)
                .column("*")
                .execute();
    }

    private long hentVedtakUtkastId(String aktorId) {
        Vedtak utkast = hentVedtak(aktorId, true);
        return utkast != null ? utkast.getId() : NO_ID;
    }

    private void oppdaterVedtakUtkast(long vedtakId, Vedtak vedtak) {
        SqlUtils.update(db, VEDTAK)
                .whereEquals(VEDTAK_ID, vedtakId)
                .set(HOVEDMAL, getName(vedtak.getHovedmal()))
                .set(INNSATSGRUPPE, getName(vedtak.getInnsatsgruppe()))
                .set(VEILEDER_IDENT, vedtak.getVeileder().getIdent())
                .set(VEILEDER_ENHET_ID, vedtak.getVeileder().getEnhetId())
                .set(SIST_OPPDATERT, DbConstants.CURRENT_TIMESTAMP)
                .set(BEGRUNNELSE, vedtak.getBegrunnelse())
                .execute();
    }

    private long lagVedtakUtkast(String aktorId, Vedtak vedtak) {
        return SqlUtils.insert(db, VEDTAK)
                .value(VEDTAK_ID, nesteFraSekvens(db, VEDTAK_SEQ))
                .value(AKTOR_ID, aktorId)
                .value(HOVEDMAL, getName(vedtak.getHovedmal()))
                .value(INNSATSGRUPPE, getName(vedtak.getInnsatsgruppe()))
                .value(VEILEDER_IDENT, vedtak.getVeileder().getIdent())
                .value(VEILEDER_ENHET_ID, vedtak.getVeileder().getEnhetId())
                .value(SIST_OPPDATERT, DbConstants.CURRENT_TIMESTAMP)
                .value(STATUS, getName(VedtakStatus.UTKAST))
                .value(BEGRUNNELSE, vedtak.getBegrunnelse())
                .execute();
    }

    @SneakyThrows
    private static Vedtak mapVedtakUtkast(ResultSet rs) {
        return new Vedtak()
                .setId(rs.getLong(VEDTAK_ID))
                .setHovedmal(valueOf(Hovedmal.class, rs.getString(HOVEDMAL)))
                .setInnsatsgruppe(valueOf(Innsatsgruppe.class, rs.getString(INNSATSGRUPPE)))
                .setVedtakStatus(valueOf(VedtakStatus.class, rs.getString(STATUS)))
                .setBegrunnelse(rs.getString(BEGRUNNELSE))
                .setSistOppdatert(rs.getTimestamp(SIST_OPPDATERT).toLocalDateTime())
                .setVeileder(
                        new Veileder()
                        .setEnhetId(rs.getString(VEILEDER_ENHET_ID))
                        .setIdent(rs.getString(VEILEDER_IDENT))
                );
    }

}
