package no.nav.veilarbvedtaksstotte.repository;

import lombok.SneakyThrows;
import no.nav.sbl.jdbc.Transactor;
import no.nav.sbl.sql.DbConstants;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import no.nav.veilarbvedtaksstotte.domain.DokumentSendtDTO;
import no.nav.veilarbvedtaksstotte.domain.Kilde;
import no.nav.veilarbvedtaksstotte.domain.Vedtak;
import no.nav.veilarbvedtaksstotte.domain.enums.Hovedmal;
import no.nav.veilarbvedtaksstotte.domain.enums.Innsatsgruppe;
import no.nav.veilarbvedtaksstotte.domain.enums.VedtakStatus;
import no.nav.veilarbvedtaksstotte.utils.EnumUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import java.sql.ResultSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Repository
public class VedtaksstotteRepository {

    public final static String VEDTAK_TABLE           = "VEDTAK";
    private final static String VEDTAK_ID             = "ID";
    private final static String SENDER                = "SENDER";
    private final static String AKTOR_ID              = "AKTOR_ID";
    private final static String HOVEDMAL              = "HOVEDMAL";
    private final static String INNSATSGRUPPE         = "INNSATSGRUPPE";
    private final static String VEILEDER_IDENT        = "VEILEDER_IDENT";
    private final static String OPPFOLGINGSENHET_ID   = "OPPFOLGINGSENHET_ID";
    private final static String SIST_OPPDATERT        = "SIST_OPPDATERT";
    private final static String BESLUTTER_IDENT       = "BESLUTTER_IDENT";
    private final static String GODKJENT_AV_BESLUTTER = "GODKJENT_AV_BESLUTTER";
    private final static String BESLUTTER_PROSESS_STARTET = "BESLUTTER_PROSESS_STARTET";
    private final static String UTKAST_OPPRETTET      = "UTKAST_OPPRETTET";
    private final static String BEGRUNNELSE           = "BEGRUNNELSE";
    private final static String STATUS                = "STATUS";
    private final static String DOKUMENT_ID           = "DOKUMENT_ID";
    private final static String JOURNALPOST_ID        = "JOURNALPOST_ID";
    private final static String GJELDENDE             = "GJELDENDE";

    private final JdbcTemplate db;
    private final KilderRepository kilderRepository;
    private final DialogRepository dialogRepository;
    private final Transactor transactor;

    @Inject
    public VedtaksstotteRepository(JdbcTemplate db, KilderRepository kilderRepository, DialogRepository dialogRepository, Transactor transactor) {
        this.db = db;
        this.kilderRepository = kilderRepository;
        this.dialogRepository = dialogRepository;
        this.transactor = transactor;
    }

    public Vedtak hentUtkastEllerFeil(String aktorId) {
        Vedtak utkast = hentUtkast(aktorId);

        if (utkast == null) {
            throw new NotFoundException("Fant ikke utkast");
        }

        return utkast;
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

        dialogRepository.slettDialogMeldinger(vedtakUtenOpplysninger.getId());
        kilderRepository.slettKilder(vedtakUtenOpplysninger.getId());

        return SqlUtils
                .delete(db, VEDTAK_TABLE)
                .where(WhereClause.equals(STATUS, EnumUtils.getName(VedtakStatus.UTKAST)).and(WhereClause.equals(AKTOR_ID,aktorId)))
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

    public void setBeslutterProsessStartet(long vedtakId) {
        String sql = "UPDATE VEDTAK SET BESLUTTER_PROSESS_STARTET = ? WHERE ID = ?";
        long itemsUpdated = db.update(sql, true, vedtakId);

        if (itemsUpdated == 0) {
            throw new RuntimeException("Fant ikke utkast å starte beslutterprosess for " + vedtakId);
        }
    }

    public void setBeslutter(long vedtakId, String beslutterIdent) {
        String sql = "UPDATE VEDTAK SET BESLUTTER_IDENT = ? WHERE ID = ?";
        long itemsUpdated = db.update(sql, beslutterIdent, vedtakId);

        if (itemsUpdated == 0) {
            throw new RuntimeException("Fant ikke utkast å sette beslutter for " + vedtakId);
        }
    }

    public void setGodkjentAvBeslutter(long vedtakId, boolean godkjent) {
        String sql = "UPDATE VEDTAK SET GODKJENT_AV_BESLUTTER = ? WHERE ID = ?";
        long itemsUpdated = db.update(sql, godkjent, vedtakId);

        if (itemsUpdated == 0) {
            throw new RuntimeException("Fant ikke utkast" + vedtakId);
        }
    }

    public Vedtak hentVedtak(long vedtakId) {
        return SqlUtils.select(db, VEDTAK_TABLE, VedtaksstotteRepository::mapVedtak)
                .where(WhereClause.equals(VEDTAK_ID, vedtakId))
                .column("*")
                .execute();
    }

    public Vedtak hentGjeldendeVedtak(String aktorId) {
        return SqlUtils.select(db, VEDTAK_TABLE, VedtaksstotteRepository::mapVedtak)
                .where(WhereClause.equals(AKTOR_ID, aktorId).and(WhereClause.equals(GJELDENDE, true)))
                .column("*")
                .execute();
    }

    public void settGjeldendeVedtakTilHistorisk(String aktorId) {
       db.update("UPDATE VEDTAK SET GJELDENDE = false WHERE AKTOR_ID = ? AND GJELDENDE = true", aktorId);
    }

    public void ferdigstillVedtak(long vedtakId, DokumentSendtDTO dokumentSendtDTO){
        SqlUtils.update(db, VEDTAK_TABLE)
            .whereEquals(VEDTAK_ID, vedtakId)
            .set(SIST_OPPDATERT, DbConstants.CURRENT_TIMESTAMP)
            .set(STATUS, EnumUtils.getName(VedtakStatus.SENDT))
            .set(DOKUMENT_ID, dokumentSendtDTO.getDokumentId())
            .set(JOURNALPOST_ID, dokumentSendtDTO.getJournalpostId())
            .set(GJELDENDE, true)
            .set(SENDER, false)
            .execute();
    }

    public void oppdaterUtkast(long vedtakId, Vedtak vedtak) {
        SqlUtils.update(db, VEDTAK_TABLE)
            .whereEquals(VEDTAK_ID, vedtakId)
            .set(HOVEDMAL, EnumUtils.getName(vedtak.getHovedmal()))
            .set(INNSATSGRUPPE, EnumUtils.getName(vedtak.getInnsatsgruppe()))
            .set(VEILEDER_IDENT, vedtak.getVeilederIdent())
            .set(OPPFOLGINGSENHET_ID, vedtak.getOppfolgingsenhetId())
            .set(SIST_OPPDATERT, DbConstants.CURRENT_TIMESTAMP)
            .set(BEGRUNNELSE, vedtak.getBegrunnelse())
            .execute();
    }

    public void opprettUtkast(String aktorId, String veilederIdent, String oppfolgingsenhetId) {
        SqlUtils.insert(db, VEDTAK_TABLE)
            .value(AKTOR_ID, aktorId)
            .value(VEILEDER_IDENT, veilederIdent)
            .value(OPPFOLGINGSENHET_ID, oppfolgingsenhetId)
            .value(SIST_OPPDATERT, DbConstants.CURRENT_TIMESTAMP)
            .value(STATUS, EnumUtils.getName(VedtakStatus.UTKAST))
            .execute();
    }

    private Vedtak hentUtkastUtenOpplysninger(String aktorId) {
        return SqlUtils.select(db, VEDTAK_TABLE, VedtaksstotteRepository::mapVedtak)
                .where(WhereClause.equals(AKTOR_ID, aktorId).and(WhereClause.equals(STATUS, EnumUtils.getName(VedtakStatus.UTKAST))))
                .column("*")
                .execute();
    }

    public void oppdaterSender(long vedtakId, boolean sender) {
        transactor.inTransaction(() -> {
            boolean lagretSender =
                    Optional.ofNullable(
                            db.queryForObject(
                                    "SELECT " + SENDER + " FROM " + VEDTAK_TABLE + " WHERE " + VEDTAK_ID + " = ? FOR UPDATE",
                                    (rs, rowNum) -> rs.getBoolean(SENDER), vedtakId))
                            .orElse(false);

            if (lagretSender == sender) {
                throw new IllegalStateException(format("Utkast med id %s er %s under sending", vedtakId, lagretSender ? "allerede" : "ikke"));
            }

            SqlUtils.update(db, VEDTAK_TABLE)
                    .whereEquals(VEDTAK_ID, vedtakId)
                    .set(SENDER, sender)
                    .execute();
        });
    }

    @SneakyThrows
    private static Vedtak mapVedtak(ResultSet rs) {
        return new Vedtak()
                .setId(rs.getLong(VEDTAK_ID))
                .setHovedmal(EnumUtils.valueOf(Hovedmal.class, rs.getString(HOVEDMAL)))
                .setInnsatsgruppe(EnumUtils.valueOf(Innsatsgruppe.class, rs.getString(INNSATSGRUPPE)))
                .setVedtakStatus(EnumUtils.valueOf(VedtakStatus.class, rs.getString(STATUS)))
                .setBegrunnelse(rs.getString(BEGRUNNELSE))
                .setSistOppdatert(rs.getTimestamp(SIST_OPPDATERT).toLocalDateTime())
                .setUtkastOpprettet(rs.getTimestamp(UTKAST_OPPRETTET).toLocalDateTime())
                .setGjeldende(rs.getBoolean(GJELDENDE))
                .setBeslutterIdent(rs.getString(BESLUTTER_IDENT))
                .setBeslutterProsessStartet(rs.getBoolean(BESLUTTER_PROSESS_STARTET))
                .setGodkjentAvBeslutter(rs.getBoolean(GODKJENT_AV_BESLUTTER))
                .setOppfolgingsenhetId(rs.getString(OPPFOLGINGSENHET_ID))
                .setVeilederIdent(rs.getString(VEILEDER_IDENT))
                .setAktorId(rs.getString(AKTOR_ID))
                .setJournalpostId(rs.getString(JOURNALPOST_ID))
                .setDokumentInfoId(rs.getString(DOKUMENT_ID))
                .setSender(rs.getBoolean(SENDER));

    }
}
