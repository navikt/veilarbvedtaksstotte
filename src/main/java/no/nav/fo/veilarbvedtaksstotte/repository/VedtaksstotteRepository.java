package no.nav.fo.veilarbvedtaksstotte.repository;

import lombok.SneakyThrows;
import no.nav.fo.veilarbvedtaksstotte.domain.DokumentSendtDTO;
import no.nav.fo.veilarbvedtaksstotte.domain.Vedtak;
import no.nav.fo.veilarbvedtaksstotte.domain.Veileder;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.Hovedmal;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.Innsatsgruppe;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.VedtakStatus;
import no.nav.sbl.sql.DbConstants;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.util.List;

import static no.nav.fo.veilarbvedtaksstotte.utils.DbUtils.nesteFraSekvens;
import static no.nav.fo.veilarbvedtaksstotte.utils.EnumUtils.getName;
import static no.nav.fo.veilarbvedtaksstotte.utils.EnumUtils.valueOf;

@Repository
public class VedtaksstotteRepository {

    private final static String VEDTAK_TABLE        = "VEDTAK";
    private final static String VEDTAK_SEQ          = "VEDTAK_SEQ";
    private final static String VEDTAK_ID           = "VEDTAK_ID";
    private final static String AKTOR_ID            = "AKTOR_ID";
    private final static String HOVEDMAL            = "HOVEDMAL";
    private final static String INNSATSGRUPPE       = "INNSATSGRUPPE";
    private final static String VEILEDER_IDENT      = "VEILEDER_IDENT";
    private final static String VEILEDER_ENHET_ID   = "VEILEDER_ENHET_ID";
    private final static String SIST_OPPDATERT      = "SIST_OPPDATERT";
    private final static String BEGRUNNELSE         = "BEGRUNNELSE";
    private final static String STATUS              = "STATUS";
    private final static String DOKUMENT_ID         = "DOKUMENT_ID";
    private final static String JOURNALPOST_ID      = "JOURNALPOST_ID";
    private final static String GJELDENDE           = "GJELDENDE";

    private final JdbcTemplate db;

    @Inject
    public VedtaksstotteRepository(JdbcTemplate db) {
        this.db = db;
    }

    public Vedtak hentUtkast(String aktorId) {
        return SqlUtils.select(db, VEDTAK_TABLE, VedtaksstotteRepository::mapVedtak)
                .where(WhereClause.equals(AKTOR_ID, aktorId).and(WhereClause.equals(STATUS, getName(VedtakStatus.UTKAST))))
                .column("*")
                .execute();
    }

    public boolean slettUtkast(String aktorId) {
        return SqlUtils
                .delete(db, VEDTAK_TABLE)
                .where(WhereClause.equals(STATUS, getName(VedtakStatus.UTKAST)).and(WhereClause.equals(AKTOR_ID,aktorId)))
                .execute() > 0;
    }

    public List<Vedtak> hentVedtak(String aktorId) {
        return SqlUtils.select(db, VEDTAK_TABLE, VedtaksstotteRepository::mapVedtak)
                .where(WhereClause.equals(AKTOR_ID, aktorId))
                .column("*")
                .executeToList();
    }

    public void settGjeldendeVedtakTilHistorisk(String aktorId) {
        SqlUtils.update(db, VEDTAK_TABLE)
            .whereEquals(AKTOR_ID, aktorId)
            .set(GJELDENDE, 0)
            .execute();
    }

    public void markerVedtakSomSendt(long vedtakId, DokumentSendtDTO dokumentSendtDTO){
        SqlUtils.update(db, VEDTAK_TABLE)
            .whereEquals(VEDTAK_ID, vedtakId)
            .set(SIST_OPPDATERT, DbConstants.CURRENT_TIMESTAMP)
            .set(STATUS, getName(VedtakStatus.SENDT))
            .set(DOKUMENT_ID, dokumentSendtDTO.getDokumentId())
            .set(JOURNALPOST_ID, dokumentSendtDTO.getJournalpostId())
            .set(GJELDENDE, 1)
            .execute();
    }

    public void oppdaterUtkast(long vedtakId, Vedtak vedtak) {
        SqlUtils.update(db, VEDTAK_TABLE)
            .whereEquals(VEDTAK_ID, vedtakId)
            .set(HOVEDMAL, getName(vedtak.getHovedmal()))
            .set(INNSATSGRUPPE, getName(vedtak.getInnsatsgruppe()))
            .set(VEILEDER_IDENT, vedtak.getVeileder().getIdent())
            .set(VEILEDER_ENHET_ID, vedtak.getVeileder().getEnhetId())
            .set(SIST_OPPDATERT, DbConstants.CURRENT_TIMESTAMP)
            .set(BEGRUNNELSE, vedtak.getBegrunnelse())
            .execute();
    }

    public void insertUtkast(String aktorId, Veileder veileder) {
        SqlUtils.insert(db, VEDTAK_TABLE)
            .value(VEDTAK_ID, nesteFraSekvens(db, VEDTAK_SEQ))
            .value(AKTOR_ID, aktorId)
            .value(VEILEDER_IDENT, veileder.getIdent())
            .value(VEILEDER_ENHET_ID, veileder.getEnhetId())
            .value(SIST_OPPDATERT, DbConstants.CURRENT_TIMESTAMP)
            .value(STATUS, getName(VedtakStatus.UTKAST))
            .execute();
    }

    @SneakyThrows
    private static Vedtak mapVedtak(ResultSet rs) {
        return new Vedtak()
                .setId(rs.getLong(VEDTAK_ID))
                .setHovedmal(valueOf(Hovedmal.class, rs.getString(HOVEDMAL)))
                .setInnsatsgruppe(valueOf(Innsatsgruppe.class, rs.getString(INNSATSGRUPPE)))
                .setVedtakStatus(valueOf(VedtakStatus.class, rs.getString(STATUS)))
                .setBegrunnelse(rs.getString(BEGRUNNELSE))
                .setSistOppdatert(rs.getTimestamp(SIST_OPPDATERT).toLocalDateTime())
                .setGjeldende(rs.getInt(GJELDENDE) == 1)
                .setVeileder(
                        new Veileder()
                        .setEnhetId(rs.getString(VEILEDER_ENHET_ID))
                        .setIdent(rs.getString(VEILEDER_IDENT))
                );
    }
}
