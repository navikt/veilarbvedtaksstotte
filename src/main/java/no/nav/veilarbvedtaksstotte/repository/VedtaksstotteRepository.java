package no.nav.veilarbvedtaksstotte.repository;

import lombok.SneakyThrows;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.NavIdent;
import no.nav.veilarbvedtaksstotte.controller.dto.SlettVedtakRequest;
import no.nav.veilarbvedtaksstotte.domain.DistribusjonBestillingId;
import no.nav.veilarbvedtaksstotte.domain.slettVedtak.SlettVedtakFeiletException;
import no.nav.veilarbvedtaksstotte.domain.vedtak.BeslutterProsessStatus;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Hovedmal;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsgruppe;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak;
import no.nav.veilarbvedtaksstotte.domain.vedtak.VedtakStatus;
import no.nav.veilarbvedtaksstotte.utils.EnumUtils;
import no.nav.veilarbvedtaksstotte.utils.SecureLog;
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
import java.util.UUID;

import static java.lang.String.format;
import static no.nav.veilarbvedtaksstotte.utils.DbUtils.queryForObjectOrNull;
import static no.nav.veilarbvedtaksstotte.utils.EnumUtils.getName;

@Repository
public class VedtaksstotteRepository {

    public static final String VEDTAK_TABLE = "VEDTAK";
    private static final String VEDTAK_ID = "ID";
    private static final String SENDER = "SENDER";
    private static final String AKTOR_ID = "AKTOR_ID";
    private static final String HOVEDMAL = "HOVEDMAL";
    private static final String INNSATSGRUPPE = "INNSATSGRUPPE";
    private static final String VEILEDER_IDENT = "VEILEDER_IDENT";
    private static final String OPPFOLGINGSENHET_ID = "OPPFOLGINGSENHET_ID";
    private static final String UTKAST_SIST_OPPDATERT = "UTKAST_SIST_OPPDATERT";
    private static final String VEDTAK_FATTET = "VEDTAK_FATTET";
    private static final String BESLUTTER_IDENT = "BESLUTTER_IDENT";
    private static final String UTKAST_OPPRETTET = "UTKAST_OPPRETTET";
    private static final String BEGRUNNELSE = "BEGRUNNELSE";
    private static final String STATUS = "STATUS";
    private static final String DOKUMENT_ID = "DOKUMENT_ID";
    private static final String JOURNALPOST_ID = "JOURNALPOST_ID";
    private static final String DOKUMENT_BESTILLING_ID = "DOKUMENT_BESTILLING_ID";
    private static final String GJELDENDE = "GJELDENDE";
    private static final String BESLUTTER_PROSESS_STATUS = "BESLUTTER_PROSESS_STATUS";
    private static final String REFERANSE = "REFERANSE";
    private static final String FEILRETTING_BEGRUNNELSE = "FEILRETTING_BEGRUNNELSE";
    private static final Integer INITIAL_DISTRIBUSJONSFORSOK_LIMIT = 12; // Prøver først 12 ganger, det tar ca. 2 timer, deretter går det over til døgnlige forsøk

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

    public List<Vedtak> hentFattedeVedtakInkludertSlettede(String aktorId) {
        String sql = format("SELECT * FROM %s WHERE %s = ? AND %s != ?", VEDTAK_TABLE, AKTOR_ID, STATUS);
        return db.query(sql, VedtaksstotteRepository::mapVedtak, aktorId, getName(VedtakStatus.UTKAST));
    }

    public List<Vedtak> hentUtkastEldreEnn(LocalDateTime dateTime) {
        String sql = format("SELECT * FROM %s WHERE %s = ? AND %s < ?", VEDTAK_TABLE, STATUS, UTKAST_SIST_OPPDATERT);
        return db.query(sql, VedtaksstotteRepository::mapVedtak, getName(VedtakStatus.UTKAST), dateTime);
    }

    public Vedtak hentUtkastEllerFeil(long vedtakId) {
        Vedtak utkast = hentVedtak(vedtakId);

        if (utkast == null || utkast.getVedtakStatus() != VedtakStatus.UTKAST) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fant ikke utkast");
        }

        return utkast;
    }

    public void slettUtkast(long vedtakId) {
        String sql = format("DELETE FROM %s WHERE %s = ? AND %s = ?", VEDTAK_TABLE, STATUS, VEDTAK_ID);
        db.update(sql, getName(VedtakStatus.UTKAST), vedtakId);
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

    public Vedtak hentSisteVedtak(AktorId aktorId) {
        String sql = format("SELECT * FROM %s WHERE %s = ? AND %s = ? ORDER BY %s DESC LIMIT 1", VEDTAK_TABLE, AKTOR_ID, STATUS, VEDTAK_FATTET);
        return queryForObjectOrNull(() -> db.queryForObject(sql, VedtaksstotteRepository::mapVedtak, aktorId.get(), VedtakStatus.SENDT.name()));
    }

    public void settGjeldendeVedtakTilHistorisk(long vedtakID) {
        db.update("UPDATE VEDTAK SET GJELDENDE = false WHERE ID = ? AND GJELDENDE = true", vedtakID);
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

    public List<Long> hentVedtakForDistribusjon(int antall) {
        var sql = "SELECT ID FROM VEDTAK" +
                " LEFT JOIN RETRY_VEDTAKDISTRIBUSJON ON VEDTAK.JOURNALPOST_ID = RETRY_VEDTAKDISTRIBUSJON.JOURNALPOST_ID" +
                " WHERE DOKUMENT_BESTILLING_ID IS NULL" +
                " AND VEDTAK_FATTET IS NOT NULL" +
                " AND VEDTAK.JOURNALPOST_ID IS NOT NULL" +
                " AND (RETRY_VEDTAKDISTRIBUSJON.DISTRIBUSJONSFORSOK < ?" +
                " OR RETRY_VEDTAKDISTRIBUSJON.DISTRIBUSJONSFORSOK IS NULL)" + // Trenger null-sjekken for å få med vedtak som aldri har hatt et mislykket forsøk
                " ORDER BY VEDTAK_FATTET ASC LIMIT ?";
        return db.queryForList(sql, Long.class, INITIAL_DISTRIBUSJONSFORSOK_LIMIT, antall);
    }

    public List<Long> hentFeilendeVedtakForDistribusjon(int antall) {
        var sql = "SELECT ID FROM VEDTAK" +
                " LEFT JOIN RETRY_VEDTAKDISTRIBUSJON ON VEDTAK.JOURNALPOST_ID = RETRY_VEDTAKDISTRIBUSJON.JOURNALPOST_ID" +
                " WHERE DOKUMENT_BESTILLING_ID IS NULL" +
                " AND VEDTAK_FATTET IS NOT NULL" +
                " AND VEDTAK.JOURNALPOST_ID IS NOT NULL" +
                " AND RETRY_VEDTAKDISTRIBUSJON.DISTRIBUSJONSFORSOK >= ?" +
                " ORDER BY VEDTAK_FATTET ASC LIMIT ?";
        return db.queryForList(sql, Long.class, INITIAL_DISTRIBUSJONSFORSOK_LIMIT, antall);
    }

    public List<Long> hentVedtakForJournalforing(int antall) {
        var sql = """
                SELECT ID FROM VEDTAK
                WHERE VEDTAK_FATTET IS NOT NULL
                AND DOKUMENT_BESTILLING_ID IS NULL
                AND JOURNALPOST_ID IS NULL
                ORDER BY VEDTAK_FATTET ASC LIMIT ?
                """;
        return db.queryForList(sql, Long.class, antall);
    }

    public UUID opprettOgHentReferanse(long vedtakId) {
        String update = format(
                "UPDATE %s SET %s = ? WHERE %s = ? AND %s is null", VEDTAK_TABLE, REFERANSE, VEDTAK_ID, REFERANSE
        );
        db.update(update, UUID.randomUUID(), vedtakId);

        String select = format("SELECT %s FROM %s WHERE %s = ?", REFERANSE, VEDTAK_TABLE, VEDTAK_ID);
        return db.queryForObject(select, UUID.class, vedtakId);
    }

    public void lagreJournalforingVedtak(long vedtakId, String journalpostId, String dokumentId) {
        String sql = format(
                "UPDATE %s SET %s = ?, %s = ? WHERE %s = ?",
                VEDTAK_TABLE, DOKUMENT_ID, JOURNALPOST_ID, VEDTAK_ID
        );
        db.update(sql, dokumentId, journalpostId, vedtakId);
    }

    public void lagreDokumentbestillingsId(long vedtakId, DistribusjonBestillingId dokumentbestillingsId) {
        String sql = format(
                "UPDATE %s SET %s = ?, %s = false WHERE %s = ?",
                VEDTAK_TABLE, DOKUMENT_BESTILLING_ID, SENDER, VEDTAK_ID
        );
        db.update(sql, dokumentbestillingsId.getId(), vedtakId);
    }

    public void ferdigstillVedtak(long vedtakId) {
        String sql = format(
                "UPDATE %s SET %s = ?, %s = CURRENT_TIMESTAMP, %s = true WHERE %s = ?",
                VEDTAK_TABLE, STATUS, VEDTAK_FATTET, GJELDENDE, VEDTAK_ID
        );
        db.update(sql, getName(VedtakStatus.SENDT), vedtakId);
    }

    /**
     * Sletter vedtak. OBS: Denne skal kun brukes dersom vi har hatt et personvernsbrudd og må 'slette' vedtaket.
     * Vi sletter aldri et vedtak i sin helhet, må alltid beholde metadata
     * @param vedtakId id til vedtaket som skal slettes
     * @param utfortAv identen til den som utfører slettingen
     * @param slettVedtakRequest request som inneholder informasjon om slettingen
     */
    public void slettVedtakVedPersonvernbrudd(long vedtakId, NavIdent utfortAv, SlettVedtakRequest slettVedtakRequest) throws SlettVedtakFeiletException {
        try {
            String begrunnelse = format("Vedtaket ble slettet %s av %s fordi %s bestilte sletting i %s på bakgrunn av at vedtaket ble fattet på feil person", LocalDateTime.now(), utfortAv.get(), slettVedtakRequest.getAnsvarligVeileder(), slettVedtakRequest.getSlettVedtakBestillingId());
            String sql = format(
                    "UPDATE %s SET %s = CURRENT_TIMESTAMP, %s = ?,  %s = ?, %s = ?, %s = false WHERE %s = ?",
                    VEDTAK_TABLE, UTKAST_SIST_OPPDATERT, FEILRETTING_BEGRUNNELSE, BEGRUNNELSE, STATUS, GJELDENDE, VEDTAK_ID
            );
            db.update(sql, begrunnelse, null, getName(VedtakStatus.SLETTET), vedtakId);
        } catch (Exception e) {
            SecureLog.getSecureLog().error("Klarte ikke å slette vedtak med id: {}", vedtakId, e);
            throw new SlettVedtakFeiletException("Klarte ikke å slette vedtak med id: " + vedtakId);
        }

    }

    public Optional<Vedtak> hentVedtakByJournalpostIdOgAktorId(String journalpostId, AktorId aktorId) {
        String sql = format("SELECT * FROM %s WHERE %s = ? AND %s = ?", VEDTAK_TABLE, JOURNALPOST_ID, AKTOR_ID);
        return Optional.ofNullable(queryForObjectOrNull(() -> db.queryForObject(sql, VedtaksstotteRepository::mapVedtak, journalpostId, aktorId.get())));
    }

    public List<AktorId> hentUnikeBrukereMedFattetVedtak() {
        String sql = format(
                "SELECT DISTINCT %s FROM %s WHERE %s = ?",
                AKTOR_ID, VEDTAK_TABLE, STATUS);

        return db.query(
                sql,
                (rs, rowNum) -> AktorId.of(rs.getString("aktor_id")),
                VedtakStatus.SENDT.name()
        );
    }

    public int hentAntallJournalforteVedtakUtenDokumentbestilling(LocalDateTime til) {
        String sql =
                "SELECT COUNT(*) FROM VEDTAK" +
                        " WHERE JOURNALPOST_ID IS NOT NULL" +
                        " AND DOKUMENT_BESTILLING_ID IS NULL" +
                        " AND VEDTAK_FATTET <= ?";

        return Optional.ofNullable(db.queryForObject(sql, Integer.class, til)).orElse(0);
    }

    public int hentFattetVedtakUtenJournalforing(LocalDateTime til) {
        String sql =
                "SELECT COUNT(*) FROM VEDTAK" +
                        " WHERE JOURNALPOST_ID IS NULL" +
                        " AND VEDTAK_FATTET <= ?";

        return Optional.ofNullable(db.queryForObject(sql, Integer.class, til)).orElse(0);
    }


    public int hentAntallVedtakMedFeilendeDokumentbestilling() {
        String sql =
                "SELECT COUNT(*) FROM VEDTAK WHERE DOKUMENT_BESTILLING_ID = ?";

        return Optional.ofNullable(
                db.queryForObject(sql, Integer.class, DistribusjonBestillingId.Feilet.INSTANCE.getId())
        ).orElse(0);
    }

    public List<Vedtak> hentFattedeVedtak(int limit, int offset) {
        String sql = format(
                "SELECT * FROM %s WHERE %s = ? ORDER BY %s ASC LIMIT %d OFFSET %d",
                VEDTAK_TABLE, STATUS, VEDTAK_ID, limit, offset
        );
        return db.query(sql, VedtaksstotteRepository::mapVedtak, getName(VedtakStatus.SENDT));
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
                .setVedtakFattet(Optional.ofNullable(rs.getTimestamp(VEDTAK_FATTET)).isPresent() ? rs.getTimestamp(VEDTAK_FATTET).toLocalDateTime() : null)
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
                .setBeslutterProsessStatus(EnumUtils.valueOf(BeslutterProsessStatus.class, rs.getString(BESLUTTER_PROSESS_STATUS)))
                .setReferanse(Optional.ofNullable(rs.getString(REFERANSE)).isPresent() ? UUID.fromString(rs.getString(REFERANSE)) : null);


    }
}
