package no.nav.fo.veilarbvedtaksstotte.repository;

import lombok.SneakyThrows;
import no.nav.fo.veilarbvedtaksstotte.domain.DokumentSendtDTO;
import no.nav.fo.veilarbvedtaksstotte.domain.Kilde;
import no.nav.fo.veilarbvedtaksstotte.domain.Vedtak;
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
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static no.nav.fo.veilarbvedtaksstotte.utils.EnumUtils.getName;
import static no.nav.fo.veilarbvedtaksstotte.utils.EnumUtils.valueOf;

@Repository
public class VedtaksstotteRepository {

    public final static String VEDTAK_TABLE         = "VEDTAK";
    private final static String VEDTAK_ID           = "ID";
    private final static String AKTOR_ID            = "AKTOR_ID";
    private final static String HOVEDMAL            = "HOVEDMAL";
    private final static String INNSATSGRUPPE       = "INNSATSGRUPPE";
    private final static String VEILEDER_IDENT      = "VEILEDER_IDENT";
    private final static String VEILEDER_ENHET_ID   = "VEILEDER_ENHET_ID";
    private final static String VEILEDER_ENHET_NAVN = "VEILEDER_ENHET_NAVN";
    private final static String SIST_OPPDATERT      = "SIST_OPPDATERT";
    private final static String BESLUTTER_NAVN      = "BESLUTTER_NAVN";
    private final static String UTKAST_OPPRETTET    = "UTKAST_OPPRETTET";
    private final static String BEGRUNNELSE         = "BEGRUNNELSE";
    private final static String STATUS              = "STATUS";
    private final static String DOKUMENT_ID         = "DOKUMENT_ID";
    private final static String JOURNALPOST_ID      = "JOURNALPOST_ID";
    private final static String GJELDENDE           = "GJELDENDE";
    private final static String SENDT_TIL_BESLUTTER = "SENDT_TIL_BESLUTTER";

    private final JdbcTemplate db;
    private KilderRepository kilderRepository;

    @Inject
    public VedtaksstotteRepository(JdbcTemplate db, KilderRepository kilderRepository) {
        this.db = db;
        this.kilderRepository = kilderRepository;
    }

    public Vedtak hentUtkast(String aktorId) {
        Vedtak vedtakUtenOpplysninger = hentUtkastUtenOpplysninger(aktorId);

        if (vedtakUtenOpplysninger == null) {
            return null;
        }

        final List<String> opplysninger = kilderRepository
                .hentKilderForVedtak(vedtakUtenOpplysninger.getId())
                .stream()
                .map(Kilde::getTekst)
                .collect(Collectors.toList());

        return vedtakUtenOpplysninger.setOpplysninger(opplysninger);
    }

    public boolean slettUtkast(String aktorId) {
        Vedtak vedtakUtenOpplysninger = hentUtkastUtenOpplysninger(aktorId);

        if (vedtakUtenOpplysninger == null) {
            return false;
        }

        kilderRepository.slettKilder(vedtakUtenOpplysninger.getId());

        return SqlUtils
                .delete(db, VEDTAK_TABLE)
                .where(WhereClause.equals(STATUS, getName(VedtakStatus.UTKAST)).and(WhereClause.equals(AKTOR_ID,aktorId)))
                .execute() > 0;
    }

    public List<Vedtak> hentVedtak(String aktorId) {
        List<Vedtak> vedtakListe = SqlUtils.select(db, VEDTAK_TABLE, VedtaksstotteRepository::mapVedtak)
                .where(WhereClause.equals(AKTOR_ID, aktorId))
                .column("*")
                .executeToList();

        List<Kilde> opplysninger = kilderRepository.hentKilderForAlleVedtak(vedtakListe);

        vedtakListe.forEach(vedtak -> {
            List<String> vedtakOpplysninger = opplysninger.stream()
                    .filter(o -> o.getVedtakId() == vedtak.getId())
                    .map(Kilde::getTekst)
                    .collect(Collectors.toList());
            vedtak.setOpplysninger(vedtakOpplysninger);
        });

        return vedtakListe;
    }

    public void markerUtkastSomSendtTilBeslutter(String aktorId, String beslutterNavn) {
        String sql = "UPDATE VEDTAK SET BESLUTTER_NAVN = ?, SENDT_TIL_BESLUTTER = true WHERE AKTOR_ID = ? AND STATUS = ?";
        long itemsUpdated = db.update(sql, beslutterNavn, aktorId, getName(VedtakStatus.UTKAST));

        if (itemsUpdated == 0) {
            throw new RuntimeException("Fant ikke utkast å markere som sendt til beslutter for bruker med akørId " + aktorId);
        }
    }

    public Vedtak hentVedtak(long vedtakId) {
        return SqlUtils.select(db, VEDTAK_TABLE, VedtaksstotteRepository::mapVedtak)
                .where(WhereClause.equals(VEDTAK_ID, vedtakId))
                .column("*")
                .execute();
    }

    public void settGjeldendeVedtakTilHistorisk(String aktorId) {
       db.update("UPDATE VEDTAK SET GJELDENDE = false WHERE AKTOR_ID = ?", aktorId);
    }

    public void settGjeldendeVedtakTilHistorisk(String aktorId, Date avsluttOppfogingDato) {
        // TODO: Er det riktig å sette SIST_OPPDATERT?
        db.update(
        "UPDATE VEDTAK SET GJELDENDE = false WHERE AKTOR_ID = ? AND SIST_OPPDATERT <= ?",
            aktorId, avsluttOppfogingDato
        );
    }

    public void ferdigstillVedtak(long vedtakId, DokumentSendtDTO dokumentSendtDTO, String beslutter){
        SqlUtils.update(db, VEDTAK_TABLE)
            .whereEquals(VEDTAK_ID, vedtakId)
            .set(SIST_OPPDATERT, DbConstants.CURRENT_TIMESTAMP)
            .set(STATUS, getName(VedtakStatus.SENDT))
            .set(DOKUMENT_ID, dokumentSendtDTO.getDokumentId())
            .set(JOURNALPOST_ID, dokumentSendtDTO.getJournalpostId())
            .set(BESLUTTER_NAVN, beslutter)
            .set(GJELDENDE, true)
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

    public void opprettUtkast(String aktorId, String veilederIdent, String veilederEnhetId, String veilederEnhetNavn) {
        SqlUtils.insert(db, VEDTAK_TABLE)
            .value(AKTOR_ID, aktorId)
            .value(VEILEDER_IDENT, veilederIdent)
            .value(VEILEDER_ENHET_ID, veilederEnhetId)
            .value(VEILEDER_ENHET_NAVN, veilederEnhetNavn)
            .value(SIST_OPPDATERT, DbConstants.CURRENT_TIMESTAMP)
            .value(STATUS, getName(VedtakStatus.UTKAST))
            .execute();
    }

    private Vedtak hentUtkastUtenOpplysninger(String aktorId) {
        return SqlUtils.select(db, VEDTAK_TABLE, VedtaksstotteRepository::mapVedtak)
                .where(WhereClause.equals(AKTOR_ID, aktorId).and(WhereClause.equals(STATUS, getName(VedtakStatus.UTKAST))))
                .column("*")
                .execute();
    }

    @SneakyThrows
    private static Vedtak mapVedtak(ResultSet rs) {
        return new Vedtak()
                .setId(rs.getInt(VEDTAK_ID))
                .setHovedmal(valueOf(Hovedmal.class, rs.getString(HOVEDMAL)))
                .setInnsatsgruppe(valueOf(Innsatsgruppe.class, rs.getString(INNSATSGRUPPE)))
                .setVedtakStatus(valueOf(VedtakStatus.class, rs.getString(STATUS)))
                .setBegrunnelse(rs.getString(BEGRUNNELSE))
                .setSistOppdatert(rs.getTimestamp(SIST_OPPDATERT).toLocalDateTime())
                .setUtkastOpprettet(rs.getTimestamp(UTKAST_OPPRETTET).toLocalDateTime())
                .setGjeldende(rs.getBoolean(GJELDENDE))
                .setSendtTilBeslutter(rs.getBoolean(SENDT_TIL_BESLUTTER))
                .setVeilederEnhetId(rs.getString(VEILEDER_ENHET_ID))
                .setVeilederIdent(rs.getString(VEILEDER_IDENT))
                .setBeslutterNavn(rs.getString(BESLUTTER_NAVN))
                .setVeilederEnhetNavn(rs.getString(VEILEDER_ENHET_NAVN))
                .setAktorId(rs.getString(AKTOR_ID))
                .setJournalpostId(rs.getString(JOURNALPOST_ID))
                .setDokumentInfoId(rs.getString(DOKUMENT_ID));

    }
}
