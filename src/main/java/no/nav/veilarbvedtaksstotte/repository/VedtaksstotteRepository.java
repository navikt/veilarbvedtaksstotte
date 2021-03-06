package no.nav.veilarbvedtaksstotte.repository;

import lombok.SneakyThrows;
import no.nav.common.types.identer.AktorId;
import no.nav.veilarbvedtaksstotte.client.dokument.DokumentSendtDTO;
import no.nav.veilarbvedtaksstotte.domain.vedtak.*;
import no.nav.veilarbvedtaksstotte.utils.EnumUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static no.nav.veilarbvedtaksstotte.utils.DbUtils.queryForObjectOrNull;
import static no.nav.veilarbvedtaksstotte.utils.EnumUtils.getName;

@Repository
public class VedtaksstotteRepository {

    public final static String VEDTAK_TABLE                 = "VEDTAK";
    private final static String VEDTAK_ID                   = "ID";
    private final static String SENDER                      = "SENDER";
    private final static String AKTOR_ID                    = "AKTOR_ID";
    private final static String HOVEDMAL                    = "HOVEDMAL";
    private final static String INNSATSGRUPPE               = "INNSATSGRUPPE";
    private final static String VEILEDER_IDENT              = "VEILEDER_IDENT";
    private final static String OPPFOLGINGSENHET_ID         = "OPPFOLGINGSENHET_ID";
    private final static String UTKAST_SIST_OPPDATERT       = "UTKAST_SIST_OPPDATERT";
    private final static String VEDTAK_FATTET               = "VEDTAK_FATTET";
    private final static String BESLUTTER_IDENT             = "BESLUTTER_IDENT";
    private final static String UTKAST_OPPRETTET            = "UTKAST_OPPRETTET";
    private final static String BEGRUNNELSE                 = "BEGRUNNELSE";
    private final static String STATUS                      = "STATUS";
    private final static String DOKUMENT_ID                 = "DOKUMENT_ID";
    private final static String JOURNALPOST_ID              = "JOURNALPOST_ID";
    private final static String DOKUMENT_BESTILLING_ID      = "DOKUMENT_BESTILLING_ID";
    private final static String GJELDENDE                   = "GJELDENDE";
    private final static String BESLUTTER_PROSESS_STATUS    = "BESLUTTER_PROSESS_STATUS";

    private final JdbcTemplate db;
    private final TransactionTemplate transactor;

    @Autowired
    public VedtaksstotteRepository(JdbcTemplate db, TransactionTemplate transactor) {
        this.db = db;
        this.transactor = transactor;
    }

    public Vedtak hentUtkast(String aktorId) {
        String sql = format("SELECT * FROM %s WHERE %s = ? AND %s = ?", VEDTAK_TABLE, AKTOR_ID, STATUS);
        return queryForObjectOrNull(() -> db.queryForObject(sql, VedtaksstotteRepository::mapVedtak, aktorId, getName(VedtakStatus.UTKAST)));
    }

    public List<Vedtak> hentFattedeVedtak(String aktorId) {
        String sql = format("SELECT * FROM %s WHERE %s = ? AND %s = ?", VEDTAK_TABLE, AKTOR_ID, STATUS);
        return db.query(sql, VedtaksstotteRepository::mapVedtak, aktorId, getName(VedtakStatus.SENDT));
    }

    public List<Vedtak> hentUtkastEldreEnn(LocalDateTime dateTime) {
        String sql = format("SELECT * FROM %s WHERE %s = ? AND %s > ?", VEDTAK_TABLE, STATUS, UTKAST_SIST_OPPDATERT);
        return db.query(sql, VedtaksstotteRepository::mapVedtak, getName(VedtakStatus.UTKAST), dateTime);
    }

    public Vedtak hentUtkastEllerFeil(long vedtakId) {
        Vedtak utkast = hentVedtak(vedtakId);

        if (utkast == null || utkast.getVedtakStatus() != VedtakStatus.UTKAST) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fant ikke utkast");
        }

        return utkast;
    }

    public boolean slettUtkast(long vedtakId) {
        String sql = format("DELETE FROM %s WHERE %s = ? AND %s = ?", VEDTAK_TABLE, STATUS, VEDTAK_ID);
        return db.update(sql, getName(VedtakStatus.UTKAST), vedtakId) > 0;
    }

    public Vedtak hentVedtak(long vedtakId) {
        String sql = format("SELECT * FROM %s WHERE %s = ?", VEDTAK_TABLE, VEDTAK_ID);
        return queryForObjectOrNull(() -> db.queryForObject(sql, VedtaksstotteRepository::mapVedtak, vedtakId));
    }

    public void setBeslutterProsessStatus(long vedtakId, BeslutterProsessStatus beslutterProsessStatus) {
        String sql = "UPDATE VEDTAK SET BESLUTTER_PROSESS_STATUS = ?::BESLUTTER_PROSESS_STATUS_TYPE WHERE ID = ?";
        long itemsUpdated = db.update(sql, getName(beslutterProsessStatus), vedtakId);

        if (itemsUpdated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fant ikke utkast å oppdatere beslutter prosess status for " + vedtakId);
        }
    }

    public void setBeslutter(long vedtakId, String beslutterIdent) {
        String sql = "UPDATE VEDTAK SET BESLUTTER_IDENT = ? WHERE ID = ?";
        long itemsUpdated = db.update(sql, beslutterIdent, vedtakId);

        if (itemsUpdated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fant ikke utkast å sette beslutter for " + vedtakId);
        }
    }

    public Vedtak hentGjeldendeVedtak(String aktorId) {
        String sql = format("SELECT * FROM %s WHERE %s = ? AND %s = true", VEDTAK_TABLE, AKTOR_ID, GJELDENDE);
        return queryForObjectOrNull(() -> db.queryForObject(sql, VedtaksstotteRepository::mapVedtak, aktorId));
    }

    public Vedtak hentSisteVedtak(String aktorId) {
        String sql = format("SELECT * FROM %s WHERE %s = ? AND %s = ? ORDER BY %s DESC LIMIT 1", VEDTAK_TABLE, AKTOR_ID, STATUS, VEDTAK_FATTET);
        return queryForObjectOrNull(() -> db.queryForObject(sql, VedtaksstotteRepository::mapVedtak, aktorId, VedtakStatus.SENDT.name()));
    }

    public void settGjeldendeVedtakTilHistorisk(String aktorId) {
       db.update("UPDATE VEDTAK SET GJELDENDE = false WHERE AKTOR_ID = ? AND GJELDENDE = true", aktorId);
    }

    public void ferdigstillVedtak(long vedtakId, DokumentSendtDTO dokumentSendtDTO){
        String sql = format(
                "UPDATE %s SET %s = ?, %s = ?, %s = ?, %s = CURRENT_TIMESTAMP, %s = true, %s = false WHERE %s = ?",
                VEDTAK_TABLE, STATUS, DOKUMENT_ID, JOURNALPOST_ID, VEDTAK_FATTET, GJELDENDE, SENDER, VEDTAK_ID
        );

        db.update(
                sql, getName(VedtakStatus.SENDT),
                dokumentSendtDTO.getDokumentId(), dokumentSendtDTO.getJournalpostId(), vedtakId
        );
    }

    public void oppdaterUtkast(long vedtakId, Vedtak vedtak) {
        String sql = format(
                "UPDATE %s SET %s = ?, %s = ?, %s = ?, %s = CURRENT_TIMESTAMP WHERE %s = ? AND %s = ?",
                VEDTAK_TABLE, HOVEDMAL, INNSATSGRUPPE, BEGRUNNELSE, UTKAST_SIST_OPPDATERT, VEDTAK_ID, STATUS
        );

        db.update(sql, getName(vedtak.getHovedmal()), getName(vedtak.getInnsatsgruppe()), vedtak.getBegrunnelse(), vedtakId, getName(VedtakStatus.UTKAST));
    }

    public void oppdaterUtkastVeileder(long vedtakId, String veilederIdent) {
        String sql = format(
                "UPDATE %s SET %s = ? WHERE %s = ? AND %s = ?",
                VEDTAK_TABLE, VEILEDER_IDENT, VEDTAK_ID, STATUS
        );

        db.update(sql, veilederIdent, vedtakId, getName(VedtakStatus.UTKAST));
    }

    public void oppdaterUtkastEnhet(long vedtakId, String enhetId) {
        String sql = format(
                "UPDATE %s SET %s = ? WHERE %s = ? AND %s = ?",
                VEDTAK_TABLE, OPPFOLGINGSENHET_ID, VEDTAK_ID, STATUS
        );

        db.update(sql, enhetId, vedtakId, getName(VedtakStatus.UTKAST));
    }

    public void opprettUtkast(String aktorId, String veilederIdent, String oppfolgingsenhetId) {
        String sql = format(
                "INSERT INTO %s(%s, %s, %s, %s, %s) values(?, ?, ?, ?, CURRENT_TIMESTAMP)",
                VEDTAK_TABLE, AKTOR_ID, VEILEDER_IDENT, OPPFOLGINGSENHET_ID, STATUS, UTKAST_SIST_OPPDATERT
        );

        db.update(sql, aktorId, veilederIdent, oppfolgingsenhetId, getName(VedtakStatus.UTKAST));
    }

    public void oppdaterSender(long vedtakId, boolean sender) {
        transactor.executeWithoutResult((status) -> {
            boolean lagretSender =
                    Optional.ofNullable(
                            db.queryForObject(
                                    format("SELECT %s FROM %s WHERE %s = ? FOR UPDATE", SENDER, VEDTAK_TABLE, VEDTAK_ID),
                                    (rs, rowNum) -> rs.getBoolean(SENDER), vedtakId))
                            .orElse(false);

            if (lagretSender == sender) {
                throw new IllegalStateException(format("Utkast med id %s er %s under sending", vedtakId, lagretSender ? "allerede" : "ikke"));
            }

            String sql = format("UPDATE %s SET %s = ? WHERE %s = ?", VEDTAK_TABLE, SENDER, VEDTAK_ID);
            db.update(sql, sender, vedtakId);
        });
    }

    public void lagreJournalforingVedtak(long vedtakId, String journalpostId, String dokumentId){
        String sql = format(
                "UPDATE %s SET %s = ?, %s = ? WHERE %s = ?",
                VEDTAK_TABLE, DOKUMENT_ID, JOURNALPOST_ID, VEDTAK_ID
        );
        db.update(sql, dokumentId, journalpostId, vedtakId);
    }

    public void lagreDokumentbestillingsId(long vedtakId, String dokumentbestillingsId){
        String sql = format(
                "UPDATE %s SET %s = ? WHERE %s = ?",
                VEDTAK_TABLE, DOKUMENT_BESTILLING_ID, VEDTAK_ID
        );
        db.update(sql, dokumentbestillingsId, vedtakId);
    }

    public void ferdigstillVedtakV2(long vedtakId){
        String sql = format(
                "UPDATE %s SET %s = ?, %s = CURRENT_TIMESTAMP, %s = true WHERE %s = ?",
                VEDTAK_TABLE, STATUS, VEDTAK_FATTET, GJELDENDE, VEDTAK_ID
        );
        db.update(sql, getName(VedtakStatus.SENDT), vedtakId);
    }


    public List<AktorId> hentUnikeBrukereMedFattetVedtakPage(int offset, int pageSize) {
        String sql = format(
                "SELECT DISTINCT %s FROM %S WHERE %s = ? ORDER BY %s LIMIT %d OFFSET %d",
                AKTOR_ID, VEDTAK_TABLE, STATUS, AKTOR_ID, pageSize, offset);
        return db.query(
                sql,
                (rs, rowNum) -> AktorId.of(rs.getString("aktor_id")),
                VedtakStatus.SENDT.name()
        );
    }

    @SneakyThrows
    private static Vedtak mapVedtak(ResultSet rs, int row) {
        return new Vedtak()
                .setId(rs.getLong(VEDTAK_ID))
                .setHovedmal(EnumUtils.valueOf(Hovedmal.class, rs.getString(HOVEDMAL)))
                .setInnsatsgruppe(EnumUtils.valueOf(Innsatsgruppe.class, rs.getString(INNSATSGRUPPE)))
                .setVedtakStatus(EnumUtils.valueOf(VedtakStatus.class, rs.getString(STATUS)))
                .setBegrunnelse(rs.getString(BEGRUNNELSE))
                .setUtkastSistOppdatert(rs.getTimestamp(UTKAST_SIST_OPPDATERT).toLocalDateTime())
                .setVedtakFattet(Optional.ofNullable(rs.getTimestamp(VEDTAK_FATTET)).isPresent()?rs.getTimestamp(VEDTAK_FATTET).toLocalDateTime():null)
                .setUtkastOpprettet(rs.getTimestamp(UTKAST_OPPRETTET).toLocalDateTime())
                .setGjeldende(rs.getBoolean(GJELDENDE))
                .setBeslutterIdent(rs.getString(BESLUTTER_IDENT))
                .setOppfolgingsenhetId(rs.getString(OPPFOLGINGSENHET_ID))
                .setVeilederIdent(rs.getString(VEILEDER_IDENT))
                .setAktorId(rs.getString(AKTOR_ID))
                .setJournalpostId(rs.getString(JOURNALPOST_ID))
                .setDokumentInfoId(rs.getString(DOKUMENT_ID))
                .setDokumentbestillingId(rs.getString(DOKUMENT_BESTILLING_ID))
                .setSender(rs.getBoolean(SENDER))
                .setBeslutterProsessStatus(EnumUtils.valueOf(BeslutterProsessStatus.class, rs.getString(BESLUTTER_PROSESS_STATUS)));

    }
}
