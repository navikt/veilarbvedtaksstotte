package no.nav.veilarbvedtaksstotte.repository;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbvedtaksstotte.client.person.OpplysningerOmArbeidssoekerMedProfilering;
import no.nav.veilarbvedtaksstotte.client.person.dto.CvDto;
import no.nav.veilarbvedtaksstotte.client.person.dto.CvErrorStatus;
import no.nav.veilarbvedtaksstotte.client.person.dto.CvInnhold;
import no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.*;
import no.nav.veilarbvedtaksstotte.utils.DbUtils;
import no.nav.veilarbvedtaksstotte.utils.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static no.nav.veilarbvedtaksstotte.utils.EnumUtils.getName;
import static no.nav.veilarbvedtaksstotte.utils.EnumUtils.valueOf;

@Repository
@Slf4j
public class OyeblikksbildeRepository {

    public final static String OYEBLIKKSBILDE_TABLE = "OYEBLIKKSBILDE";
    private final static String VEDTAK_ID = "VEDTAK_ID";
    private final static String OYEBLIKKSBILDE_TYPE = "OYEBLIKKSBILDE_TYPE";
    private final static String JSON = "JSON";
    private final static String DOKUMENT_ID = "DOKUMENT_ID";

    private final JdbcTemplate db;

    @Autowired
    public OyeblikksbildeRepository(JdbcTemplate db) {
        this.db = db;
    }

    public List<OyeblikksbildeDto> hentOyeblikksbildeForVedtak(long vedtakId) {
        String sql = format("SELECT * FROM %s WHERE %s = ?", OYEBLIKKSBILDE_TABLE, VEDTAK_ID);
        return db.query(sql, OyeblikksbildeRepository::mapOyeblikksbilde, vedtakId);
    }

    public Optional<OyeblikksbildeCvDto> hentCVOyeblikksbildeForVedtak(long vedtakId) {
        try {
            String sql = format("SELECT * FROM %s WHERE %s = ? AND OYEBLIKKSBILDE_TYPE = ?::OYEBLIKKSBILDE_TYPE", OYEBLIKKSBILDE_TABLE, VEDTAK_ID);
            return Optional.ofNullable(db.queryForObject(sql, OyeblikksbildeRepository::mapCvOyeblikksbilde, vedtakId, OyeblikksbildeType.CV_OG_JOBBPROFIL.name()));
        } catch (Exception e) {
            log.warn("Kan ikke hente oyeblikksbilde " + e, e);
            return Optional.empty();
        }
    }

    public Optional<OyeblikksbildeRegistreringDto> hentRegistreringOyeblikksbildeForVedtak(long vedtakId) {
        try {
            String sql = format("SELECT * FROM %s WHERE %s = ? AND OYEBLIKKSBILDE_TYPE = ?::OYEBLIKKSBILDE_TYPE", OYEBLIKKSBILDE_TABLE, VEDTAK_ID);
            return Optional.ofNullable(db.queryForObject(sql, OyeblikksbildeRepository::mapRegistreringOyeblikksbilde, vedtakId, OyeblikksbildeType.REGISTRERINGSINFO.name()));
        } catch (Exception e) {
            log.warn("Kan ikke hente oyeblikksbilde " + e, e);
            return Optional.empty();
        }
    }

    public Optional<OyeblikksbildeArbeidssokerRegistretDto> hentArbeidssokerRegistretOyeblikksbildeForVedtak(long vedtakId) {
        try {
            String sql = format("SELECT * FROM %s WHERE %s = ? AND OYEBLIKKSBILDE_TYPE = ?::OYEBLIKKSBILDE_TYPE", OYEBLIKKSBILDE_TABLE, VEDTAK_ID);
            return Optional.ofNullable(db.queryForObject(sql, OyeblikksbildeRepository::mapArbeidssokerRegisteretOyeblikksbilde, vedtakId, OyeblikksbildeType.ARBEIDSSOKERREGISTRET.name()));
        } catch (Exception e) {
            log.warn("Kan ikke hente oyeblikksbilde " + e, e);
            return Optional.empty();
        }
    }

    public Optional<OyeblikksbildeEgenvurderingDto> hentEgenvurderingOyeblikksbildeForVedtak(long vedtakId) {
        try {
            NamedParameterJdbcTemplate namedDb = new NamedParameterJdbcTemplate(db);

            // I teorien skal det aldri finnes mer enn én egenvurdering per vedtak, denne prioriterer V2 hvis det allikevel skulle skje
            String sql =
                    "SELECT * FROM " + OYEBLIKKSBILDE_TABLE + " " +
                            "WHERE " + VEDTAK_ID + " = :vedtakId " +
                            "AND " + OYEBLIKKSBILDE_TYPE + " IN (:typeV1::OYEBLIKKSBILDE_TYPE, :typeV2::OYEBLIKKSBILDE_TYPE) " +
                            "ORDER BY CASE " + OYEBLIKKSBILDE_TYPE + " WHEN 'EGENVURDERING_V2' THEN 0 ELSE 1 END " +
                            "LIMIT 1";

            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("vedtakId", vedtakId)
                    .addValue("typeV1", OyeblikksbildeType.EGENVURDERING.name())
                    .addValue("typeV2", OyeblikksbildeType.EGENVURDERING_V2.name());

            OyeblikksbildeEgenvurderingDto result = namedDb.queryForObject(
                    sql, params, OyeblikksbildeRepository::mapEgenvurderingOyeblikksbilde);
            return Optional.ofNullable(result);
        } catch (Exception e) {
            log.warn("Kan ikke hente oyeblikksbilde " + e, e);
            return Optional.empty();
        }
    }

    public void slettOyeblikksbilder(long vedtakId) {
        db.update(format("DELETE FROM %s WHERE %s = %d", OYEBLIKKSBILDE_TABLE, VEDTAK_ID, vedtakId));
    }

    @SneakyThrows
    public void upsertCVOyeblikksbilde(long vedtakId, CvDto cvDto) {
        String jsonCvDto = "";
        if (cvDto instanceof CvDto.CVMedInnhold) {
            jsonCvDto = JsonUtils.getObjectMapper().writeValueAsString(((CvDto.CVMedInnhold) cvDto).getCvInnhold());
        } else if (cvDto instanceof CvDto.CvMedError) {
            CvErrorStatus cvErrorStatus = ((CvDto.CvMedError) cvDto).getCvErrorStatus();
            jsonCvDto = getNoDataMessageForCV(cvErrorStatus);
        }

        Optional<OyeblikksbildeDto> oyeblikksbilde = hentOyeblikksbilde(vedtakId, OyeblikksbildeType.CV_OG_JOBBPROFIL);

        if (oyeblikksbilde.isPresent()) {
            db.update(
                    "UPDATE OYEBLIKKSBILDE SET JSON = ?::json WHERE VEDTAK_ID = ? AND OYEBLIKKSBILDE_TYPE = ?::OYEBLIKKSBILDE_TYPE",
                    jsonCvDto, vedtakId, OyeblikksbildeType.CV_OG_JOBBPROFIL.name()
            );
        } else {
            db.update(
                    "INSERT INTO OYEBLIKKSBILDE (VEDTAK_ID, OYEBLIKKSBILDE_TYPE, JSON) VALUES (?,?::OYEBLIKKSBILDE_TYPE,?::json)",
                    vedtakId, OyeblikksbildeType.CV_OG_JOBBPROFIL.name(), jsonCvDto
            );
        }
    }

    @SneakyThrows
    public void upsertArbeidssokerRegistretOyeblikksbilde(long vedtakId, OpplysningerOmArbeidssoekerMedProfilering registreringsdataDto) {
        String jsonRegistreringDto = JsonUtils.getObjectMapper().writeValueAsString(registreringsdataDto);

        if (registreringsdataDto == null || jsonRegistreringDto == null || jsonRegistreringDto.isEmpty()) {
            jsonRegistreringDto = """
                    {"ingenData": "Personen har ikke registrert noen svar."}
                    """;
        }

        Optional<OyeblikksbildeDto> oyeblikksbilde = hentOyeblikksbilde(vedtakId, OyeblikksbildeType.ARBEIDSSOKERREGISTRET);

        if (oyeblikksbilde.isPresent()) {
            db.update(
                    "UPDATE OYEBLIKKSBILDE SET JSON = ?::json WHERE VEDTAK_ID = ? AND OYEBLIKKSBILDE_TYPE = ?::OYEBLIKKSBILDE_TYPE",
                    jsonRegistreringDto, vedtakId, OyeblikksbildeType.ARBEIDSSOKERREGISTRET.name()
            );
        } else {
            db.update(
                    "INSERT INTO OYEBLIKKSBILDE (VEDTAK_ID, OYEBLIKKSBILDE_TYPE, JSON) VALUES (?,?::OYEBLIKKSBILDE_TYPE,?::json)",
                    vedtakId, OyeblikksbildeType.ARBEIDSSOKERREGISTRET.name(), jsonRegistreringDto
            );
        }
    }

    @SneakyThrows
    public void upsertEgenvurderingOyeblikksbilde(long vedtakId, EgenvurderingDto egenvurderingDto) {
        String jsonEgenvurderingDto = JsonUtils.getObjectMapper().writeValueAsString(egenvurderingDto);
        if (egenvurderingDto == null || jsonEgenvurderingDto == null || jsonEgenvurderingDto.isEmpty()) {
            jsonEgenvurderingDto = """
                    {"ingenData": "Personen har ikke registrert svar om behov for veiledning."}
                    """;
        }
        Optional<OyeblikksbildeDto> oyeblikksbilde = hentOyeblikksbilde(vedtakId, OyeblikksbildeType.EGENVURDERING);

        if (oyeblikksbilde.isPresent()) {
            db.update(
                    "UPDATE OYEBLIKKSBILDE SET JSON = ?::json WHERE VEDTAK_ID = ? AND OYEBLIKKSBILDE_TYPE = ?::OYEBLIKKSBILDE_TYPE",
                    jsonEgenvurderingDto, vedtakId, OyeblikksbildeType.EGENVURDERING.name()
            );
        } else {
            db.update(
                    "INSERT INTO OYEBLIKKSBILDE (VEDTAK_ID, OYEBLIKKSBILDE_TYPE, JSON) VALUES (?,?::OYEBLIKKSBILDE_TYPE,?::json)",
                    vedtakId, OyeblikksbildeType.EGENVURDERING.name(), jsonEgenvurderingDto
            );
        }
    }

    @SneakyThrows
    public void upsertEgenvurderingV2Oyeblikksbilde(long vedtakId, EgenvurderingV2Dto egenvurderingV2Dto) {
        String jsonEgenvurderingV2Dto = JsonUtils.getObjectMapper().writeValueAsString(egenvurderingV2Dto);
        if (egenvurderingV2Dto == null || jsonEgenvurderingV2Dto == null || jsonEgenvurderingV2Dto.isEmpty()) {
            jsonEgenvurderingV2Dto = """
                    {"ingenData": "Personen har ikke registrert svar om behov for veiledning."}
                    """;
        }
        Optional<OyeblikksbildeDto> oyeblikksbilde = hentOyeblikksbilde(vedtakId, OyeblikksbildeType.EGENVURDERING_V2);

        if (oyeblikksbilde.isPresent()) {
            db.update(
                    "UPDATE OYEBLIKKSBILDE SET JSON = ?::json WHERE VEDTAK_ID = ? AND OYEBLIKKSBILDE_TYPE = ?::OYEBLIKKSBILDE_TYPE",
                    jsonEgenvurderingV2Dto, vedtakId, OyeblikksbildeType.EGENVURDERING_V2.name()
            );
        } else {
            db.update(
                    "INSERT INTO OYEBLIKKSBILDE (VEDTAK_ID, OYEBLIKKSBILDE_TYPE, JSON) VALUES (?,?::OYEBLIKKSBILDE_TYPE,?::json)",
                    vedtakId, OyeblikksbildeType.EGENVURDERING_V2.name(), jsonEgenvurderingV2Dto
            );
        }
    }

    private Optional<OyeblikksbildeDto> hentOyeblikksbilde(long vedtakId, OyeblikksbildeType type) {
        String sql = format("SELECT * FROM %s WHERE %s = ? AND %s = ?::OYEBLIKKSBILDE_TYPE LIMIT 1", OYEBLIKKSBILDE_TABLE, VEDTAK_ID, OYEBLIKKSBILDE_TYPE);
        return Optional.ofNullable(DbUtils.queryForObjectOrNull(
                () -> db.queryForObject(sql, OyeblikksbildeRepository::mapOyeblikksbilde, vedtakId, getName(type)))
        );
    }

    public void lagreJournalfortDokumentId(long vedtakId, String dokumentId, OyeblikksbildeType oyeblikksbildeType) {
        db.update(
                "UPDATE OYEBLIKKSBILDE SET dokument_id = ?  WHERE VEDTAK_ID = ? AND OYEBLIKKSBILDE_TYPE = ?::OYEBLIKKSBILDE_TYPE",
                dokumentId, vedtakId, oyeblikksbildeType.name()
        );
    }

    @SneakyThrows
    private static OyeblikksbildeDto mapOyeblikksbilde(ResultSet rs, int row) {
        OyeblikksbildeType oyeblikksbildeType = valueOf(OyeblikksbildeType.class, rs.getString(OYEBLIKKSBILDE_TYPE));
        return new OyeblikksbildeDto()
                .setVedtakId(rs.getLong(VEDTAK_ID))
                .setOyeblikksbildeType(oyeblikksbildeType)
                .setJson(rs.getString(JSON))
                .setJournalfort(rs.getString(DOKUMENT_ID) != null && !rs.getString(DOKUMENT_ID).isEmpty());
    }

    @SneakyThrows
    private static OyeblikksbildeCvDto mapCvOyeblikksbilde(ResultSet rs, int row) {
        CvInnhold cvInnhold = JsonUtils.fromJson(rs.getString(JSON), CvInnhold.class);
        return new OyeblikksbildeCvDto()
                .setData(cvInnhold)
                .setJournalfort(rs.getString(DOKUMENT_ID) != null && !rs.getString(DOKUMENT_ID).isEmpty());
    }

    @SneakyThrows
    private static OyeblikksbildeArbeidssokerRegistretDto mapArbeidssokerRegisteretOyeblikksbilde(ResultSet rs, int row) {
        OpplysningerOmArbeidssoekerMedProfilering registreringsdataDto = JsonUtils.fromJson(rs.getString(JSON), OpplysningerOmArbeidssoekerMedProfilering.class);
        return new OyeblikksbildeArbeidssokerRegistretDto()
                .setData(registreringsdataDto)
                .setJournalfort(rs.getString(DOKUMENT_ID) != null && !rs.getString(DOKUMENT_ID).isEmpty());
    }

    @SneakyThrows
    private static OyeblikksbildeEgenvurderingDto mapEgenvurderingOyeblikksbilde(ResultSet rs, int row) {
        String json = rs.getString(JSON);
        EgenvurderingData data;
        try {
            JsonUtils.fromJson(json, IngenDataDto.class);
            data = null;
        } catch (Exception e1) {
            try {
                //Prøv V2 først
                data = JsonUtils.fromJson(json, EgenvurderingV2Dto.class);
            } catch (Exception e2) {
                data = JsonUtils.fromJson(json, EgenvurderingDto.class);
            }
        }

        return new OyeblikksbildeEgenvurderingDto()
                .setData(data)
                .setJournalfort(rs.getString(DOKUMENT_ID) != null && !rs.getString(DOKUMENT_ID).isEmpty())
                .setType(OyeblikksbildeType.valueOf(rs.getString(OYEBLIKKSBILDE_TYPE)));
    }

    private static String getNoDataMessageForCV(CvErrorStatus cvErrorStatus) {
        return switch (cvErrorStatus) {
            case IKKE_DELT -> "{\"ingenData\": \"Personen har ikke delt CV.\"}";
            case IKKE_FYLT_UT -> "{\"ingenData\": \"Personen har ikke fylt ut CV.\"}";
        };
    }

    public String hentJournalfortDokumentId(long vedtakId, OyeblikksbildeType oyeblikksbildeType) {
        String sql = format("SELECT dokument_id FROM %s WHERE %s = ? AND OYEBLIKKSBILDE_TYPE = ?::OYEBLIKKSBILDE_TYPE", OYEBLIKKSBILDE_TABLE, VEDTAK_ID);
        return db.queryForObject(sql, String.class, vedtakId, oyeblikksbildeType.name());
    }

    @SneakyThrows
    private static OyeblikksbildeRegistreringDto mapRegistreringOyeblikksbilde(ResultSet rs, int row) {
        RegistreringResponseDto registreringsdataDto = JsonUtils.fromJson(rs.getString(JSON), RegistreringResponseDto.class);
        return new OyeblikksbildeRegistreringDto(
                registreringsdataDto,
                rs.getString(DOKUMENT_ID) != null && !rs.getString(DOKUMENT_ID).isEmpty()
        );

    }

}

