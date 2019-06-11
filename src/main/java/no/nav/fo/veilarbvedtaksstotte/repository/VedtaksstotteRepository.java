package no.nav.fo.veilarbvedtaksstotte.repository;

import lombok.SneakyThrows;
import no.nav.fo.veilarbvedtaksstotte.domain.DokumentSendtDTO;
import no.nav.fo.veilarbvedtaksstotte.domain.KafkaAvsluttOppfolging;
import no.nav.fo.veilarbvedtaksstotte.domain.Opplysning;
import no.nav.fo.veilarbvedtaksstotte.domain.Vedtak;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.Hovedmal;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.Innsatsgruppe;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.VedtakStatus;
import no.nav.fo.veilarbvedtaksstotte.kafka.AvsluttOpfolgingTemplate;
import no.nav.sbl.sql.DbConstants;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.util.List;
import java.util.stream.Collectors;

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
    private OpplysningerRepository opplysningerRepository;

    @Inject
    public VedtaksstotteRepository(JdbcTemplate db, OpplysningerRepository opplysningerRepository) {
        this.db = db;
        this.opplysningerRepository = opplysningerRepository;
    }


    public Vedtak hentUtkast(String aktorId) {
        Vedtak vedtakUtenOpplysninger = SqlUtils.select(db, VEDTAK_TABLE, VedtaksstotteRepository::mapVedtak)
                .where(WhereClause.equals(AKTOR_ID, aktorId).and(WhereClause.equals(STATUS, getName(VedtakStatus.UTKAST))))
                .column("*")
                .execute();

        if (vedtakUtenOpplysninger == null) {
            return null;
        }

        final List<String> opplysninger = opplysningerRepository
                .hentOpplysningerForVedtak(vedtakUtenOpplysninger.getId())
                .stream()
                .map(Opplysning::getTekst)
                .collect(Collectors.toList());

        return vedtakUtenOpplysninger.setOpplysninger(opplysninger);
    }

    public boolean slettUtkast(String aktorId) {
        return SqlUtils
                .delete(db, VEDTAK_TABLE)
                .where(WhereClause.equals(STATUS, getName(VedtakStatus.UTKAST)).and(WhereClause.equals(AKTOR_ID,aktorId)))
                .execute() > 0;
    }

    public boolean slettUtkast(KafkaAvsluttOppfolging kafkaMelding) {
        return SqlUtils
                .delete(db, VEDTAK_TABLE)
                .where(WhereClause.equals(STATUS, getName(VedtakStatus.UTKAST))
                        .and(WhereClause.equals(AKTOR_ID,kafkaMelding.getAktoerId()))
                        .and(WhereClause.lteq(SIST_OPPDATERT, kafkaMelding.getSluttdato())))
                .execute() > 0;
    }

    public List<Vedtak> hentVedtak(String aktorId) {
        List<Vedtak> vedtakListe = SqlUtils.select(db, VEDTAK_TABLE, VedtaksstotteRepository::mapVedtak)
                .where(WhereClause.equals(AKTOR_ID, aktorId))
                .column("*")
                .executeToList();

        List<Opplysning> opplysninger = opplysningerRepository.hentOpplysningerForAlleVedtak(vedtakListe);

        vedtakListe.forEach(vedtak -> {
            List<String> vedtakOpplysninger = opplysninger.stream()
                    .filter(o -> o.getVedtakId() == vedtak.getId())
                    .map(Opplysning::getTekst)
                    .collect(Collectors.toList());
            vedtak.setOpplysninger(vedtakOpplysninger);
        });

        return vedtakListe;
    }

    public Vedtak hentVedtak(long vedtakId) {
        return SqlUtils.select(db, VEDTAK_TABLE, VedtaksstotteRepository::mapVedtak)
                .where(WhereClause.equals(VEDTAK_ID, vedtakId))
                .column("*")
                .execute();
    }

    public void settGjeldendeVedtakTilHistorisk(String aktorId) {
        SqlUtils.update(db, VEDTAK_TABLE)
            .whereEquals(AKTOR_ID, aktorId)
            .set(GJELDENDE, 0)
            .execute();
    }

    public void settGjeldendeVedtakTilHistorisk(KafkaAvsluttOppfolging kafkaMelding) {
        String sqlQuery = String.format("UPDATE %s SET %s = 0 WHERE %s = %s AND = %s <= %s",
                VEDTAK_TABLE, GJELDENDE, AKTOR_ID, kafkaMelding.getAktoerId(), SIST_OPPDATERT, kafkaMelding.getSluttdato());
        db.update(sqlQuery);
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
            .set(VEILEDER_IDENT, vedtak.getVeilederIdent())
            .set(VEILEDER_ENHET_ID, vedtak.getVeilederEnhetId())
            .set(SIST_OPPDATERT, DbConstants.CURRENT_TIMESTAMP)
            .set(BEGRUNNELSE, vedtak.getBegrunnelse())
            .execute();
    }

    public void insertUtkast(String aktorId, String veilederIdent, String veilederEnhetId) {
        SqlUtils.insert(db, VEDTAK_TABLE)
                .value(VEDTAK_ID, nesteFraSekvens(db, VEDTAK_SEQ))
                .value(AKTOR_ID, aktorId)
                .value(VEILEDER_IDENT, veilederIdent)
                .value(VEILEDER_ENHET_ID, veilederEnhetId)
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
                .setVeilederEnhetId(rs.getString(VEILEDER_ENHET_ID))
                .setVeilederIdent(rs.getString(VEILEDER_IDENT))
                .setAktorId(rs.getString(AKTOR_ID))
                .setJournalpostId(rs.getString(JOURNALPOST_ID))
                .setDokumentInfoId(rs.getString(DOKUMENT_ID));

    }
}
