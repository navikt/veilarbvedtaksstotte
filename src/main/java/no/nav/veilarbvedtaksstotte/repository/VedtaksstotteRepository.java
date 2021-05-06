package no.nav.veilarbvedtaksstotte.repository;

import lombok.SneakyThrows;
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
import java.util.stream.Collectors;

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

    public UtkastetVedtak hentUtkast(String aktorId) {
        String sql = format("SELECT * FROM %s WHERE %s = ? AND %s = ?", VEDTAK_TABLE, AKTOR_ID, STATUS);
        VedtakEntity vedtakEntity = queryForObjectOrNull(() -> db.queryForObject(
                sql,
                VedtaksstotteRepository::mapVedtakEntity,
                aktorId, getName(VedtakStatus.UTKAST)
        ));

        if (vedtakEntity == null) {
            return null;
        }

        return mapUtkastVedtak(vedtakEntity);
    }

    public List<FattetVedtak> hentFattedeVedtak(String aktorId) {
        String sql = format("SELECT * FROM %s WHERE %s = ? AND %s = ?", VEDTAK_TABLE, AKTOR_ID, STATUS);
        List<VedtakEntity> vedtakEntities = db.query(sql,
                VedtaksstotteRepository::mapVedtakEntity,
                aktorId,
                getName(VedtakStatus.SENDT));

        return vedtakEntities
                .stream()
                .map(VedtaksstotteRepository::mapFattetVedtak)
                .collect(Collectors.toList());
    }

    public List<UtkastetVedtak> hentUtkastEldreEnn(LocalDateTime dateTime) {
        String sql = format("SELECT * FROM %s WHERE %s = ? AND %s > ?", VEDTAK_TABLE, STATUS, UTKAST_SIST_OPPDATERT);
        List<VedtakEntity> vedtakEntities = db.query(sql,
                VedtaksstotteRepository::mapVedtakEntity,
                getName(VedtakStatus.UTKAST),
                dateTime
        );

        return vedtakEntities
                .stream()
                .map(VedtaksstotteRepository::mapUtkastVedtak)
                .collect(Collectors.toList());
    }

    public VedtakEntity hentVedtakEntity(long vedtakId) {
        String sql = format("SELECT * FROM %s WHERE %s = ?", VEDTAK_TABLE, VEDTAK_ID);
        return queryForObjectOrNull(() -> db.queryForObject(
                sql,
                VedtaksstotteRepository::mapVedtakEntity,
                vedtakId)
        );
    }

    public UtkastetVedtak hentUtkastEllerFeil(long vedtakId) {
        VedtakEntity vedtakEntity = hentVedtakEntity(vedtakId);

        if (vedtakEntity == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fant ikke Utkastet Vedtak");
        }
        return mapUtkastVedtak(vedtakEntity);
    }

    public FattetVedtak hentFattetEllerFeil(long vedtakId) {
        VedtakEntity vedtakEntity = hentVedtakEntity(vedtakId);

        if (vedtakEntity == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fant ikke Fattet Vedtak");
        }

        return mapFattetVedtak(vedtakEntity);
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

    public void ferdigstillVedtakV2(long vedtakId) {
        String sql = format(
                "UPDATE %s SET %s = ?, %s = CURRENT_TIMESTAMP, %s = true WHERE %s = ?",
                VEDTAK_TABLE, STATUS, VEDTAK_FATTET, GJELDENDE, VEDTAK_ID
        );
        db.update(sql, getName(VedtakStatus.SENDT), vedtakId);
    }

    @SneakyThrows
    private static Vedtak mapVedtak(ResultSet rs, int rowId) {
        VedtakEntity vedtakEntity = mapVedtakEntity(rs, rowId);
        String vedtakStatus = vedtakEntity.getVedtakStatus().name();

        switch (vedtakStatus) {
            case "UTKAST":
                return mapUtkastVedtak(vedtakEntity);

            case "SENDT":
                return mapFattetVedtak(vedtakEntity);
            default:
                return null;
        }
    }

    @SneakyThrows
    private static VedtakEntity mapVedtakEntity(ResultSet rs, int rowId) {
        return VedtakEntity.builder()
                .id(rs.getLong(VEDTAK_ID))
                .aktorId(rs.getString(AKTOR_ID))
                .hovedmal(EnumUtils.valueOf(Hovedmal.class, rs.getString(HOVEDMAL)))
                .innsatsgruppe(EnumUtils.valueOf(Innsatsgruppe.class, rs.getString(INNSATSGRUPPE)))
                .begrunnelse(rs.getString(BEGRUNNELSE))
                .veilederIdent(rs.getString(VEILEDER_IDENT))
                .oppfolgingsenhetId(rs.getString(OPPFOLGINGSENHET_ID))
                .beslutterIdent(rs.getString(BESLUTTER_IDENT))
                .vedtakStatus(VedtakStatus.valueOf(rs.getString(STATUS)))
                .vedtakFattet(Optional.ofNullable(rs.getTimestamp(VEDTAK_FATTET)).isPresent() ? rs.getTimestamp(VEDTAK_FATTET).toLocalDateTime() : null)
                .gjeldende(rs.getBoolean(GJELDENDE))
                .journalpostId(rs.getString(JOURNALPOST_ID))
                .dokumentInfoId(rs.getString(DOKUMENT_ID))
                .dokumentbestillingId(rs.getString(DOKUMENT_BESTILLING_ID))
                .sistOppdatert(rs.getTimestamp(UTKAST_SIST_OPPDATERT).toLocalDateTime())
                .utkastOpprettet(rs.getTimestamp(UTKAST_OPPRETTET).toLocalDateTime())
                .beslutterProsessStatus(EnumUtils.valueOf(BeslutterProsessStatus.class, rs.getString(BESLUTTER_PROSESS_STATUS)))
                .build();
    }

    @SneakyThrows
    public static FattetVedtak mapFattetVedtak(VedtakEntity vedtakEntity) {
        return FattetVedtak.builder()
                .id(vedtakEntity.getId())
                .aktorId(vedtakEntity.getAktorId())
                .hovedmal(vedtakEntity.getHovedmal())
                .innsatsgruppe(vedtakEntity.getInnsatsgruppe())
                .veilederIdent(vedtakEntity.getVeilederIdent())
                .veilederNavn(vedtakEntity.getVeilederNavn())
                .oppfolgingsenhetId(vedtakEntity.getOppfolgingsenhetId())
                .oppfolgingsenhetNavn(vedtakEntity.getOppfolgingsenhetNavn())
                .opplysninger(vedtakEntity.getOpplysninger())
                .begrunnelse(vedtakEntity.getBegrunnelse())
                .beslutterIdent(vedtakEntity.getBeslutterIdent())
                .beslutterNavn(vedtakEntity.getBeslutterNavn())
                .vedtakFattet(vedtakEntity.getVedtakFattet())
                .gjeldende(vedtakEntity.isGjeldende())
                .dokumentbestillingId(vedtakEntity.getDokumentbestillingId())
                .dokumentInfoId(vedtakEntity.getDokumentInfoId())
                .journalpostId(vedtakEntity.getJournalpostId())
                .vedtakStatus(vedtakEntity.getVedtakStatus())
                .build();
    }

    @SneakyThrows
    public static UtkastetVedtak mapUtkastVedtak(VedtakEntity vedtakEntity) {
        return UtkastetVedtak.builder()
                .id(vedtakEntity.getId())
                .aktorId(vedtakEntity.getAktorId())
                .hovedmal(vedtakEntity.getHovedmal())
                .innsatsgruppe(vedtakEntity.getInnsatsgruppe())
                .veilederIdent(vedtakEntity.getVeilederIdent())
                .veilederNavn(vedtakEntity.getVeilederNavn())
                .oppfolgingsenhetId(vedtakEntity.getOppfolgingsenhetId())
                .oppfolgingsenhetNavn(vedtakEntity.getOppfolgingsenhetNavn())
                .opplysninger(vedtakEntity.getOpplysninger())
                .begrunnelse(vedtakEntity.getBegrunnelse())
                .beslutterIdent(vedtakEntity.getBeslutterIdent())
                .beslutterNavn(vedtakEntity.getBeslutterNavn())
                .sistOppdatert(vedtakEntity.getSistOppdatert())
                .utkastOpprettet(vedtakEntity.getUtkastOpprettet())
                .beslutterProsessStatus(vedtakEntity.getBeslutterProsessStatus())
                .vedtakStatus(vedtakEntity.getVedtakStatus())
                .build();
    }
}
