package no.nav.veilarbvedtaksstotte.repository;

import no.nav.veilarbvedtaksstotte.domain.vedtak.VedtakStatus;
import no.nav.veilarbvedtaksstotte.utils.DbTestUtils;
import no.nav.veilarbvedtaksstotte.utils.PostgresContainer;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

import static no.nav.veilarbvedtaksstotte.domain.vedtak.VedtakStatus.SENDT;
import static no.nav.veilarbvedtaksstotte.domain.vedtak.VedtakStatus.UTKAST;
import static no.nav.veilarbvedtaksstotte.utils.DbUtils.queryForObjectOrNull;
import static no.nav.veilarbvedtaksstotte.utils.TestData.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


public class FlywayDbMigrationTest {
    private JdbcTemplate db;
    private PostgresContainer postgresContainer;

    @Before
    public void steUp() {
        postgresContainer = new PostgresContainer();
        db = postgresContainer.createJdbcTemplate();
    }

    @Test
    public void testVersion_for_og_etter_colon_endringene_til_v23() {
        final String versjonPrefix = "22";
        final String versjonEtterVedtakOppdatert = "23";
        prepareDatabasen(versjonPrefix);
        LocalDateTime utkastDate = LocalDateTime.now();
        LocalDateTime fattetDate = LocalDateTime.now().minusDays(1);
        String sql =
                "INSERT INTO VEDTAK(AKTOR_ID, VEILEDER_IDENT, OPPFOLGINGSENHET_ID, STATUS, SIST_OPPDATERT)"
                        + " values(?, ?, ?, ?, ?)";
        db.update(sql, TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID, SENDT.name(), fattetDate);
        db.update(sql, TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID, UTKAST.name(), utkastDate);
        LocalDateTime sistOppdatertFattet = hentDateKolonne("SIST_OPPDATERT", SENDT);
        LocalDateTime sistOppdatertUtkast = hentDateKolonne("SIST_OPPDATERT", UTKAST);
        assertEquals(fattetDate, sistOppdatertFattet);
        assertEquals(utkastDate, sistOppdatertUtkast);
        prepareDatabasen(versjonEtterVedtakOppdatert);
        LocalDateTime utkastSistOppdatertFattet = hentDateKolonne("UTKAST_SIST_OPPDATERT", SENDT);
        LocalDateTime vedtakFattetFattet = hentDateKolonne("VEDTAK_FATTET", SENDT);
        LocalDateTime utkastSistOppdatertUtkast = hentDateKolonne("UTKAST_SIST_OPPDATERT", UTKAST);
        LocalDateTime vedtakFattetUtkast = hentDateKolonne("VEDTAK_FATTET", UTKAST);
        assertEquals(sistOppdatertFattet, utkastSistOppdatertFattet);
        assertEquals(sistOppdatertFattet, vedtakFattetFattet);
        assertEquals(sistOppdatertUtkast, utkastSistOppdatertUtkast);
        assertNull("Utkast skal ikke ha verdi for VEDTAK_FATTET", vedtakFattetUtkast);
    }

    private LocalDateTime hentDateKolonne(String dateKolonne, VedtakStatus vedtakStatus) {
        return queryForObjectOrNull(() ->
                db.queryForObject(
                        "SELECT * FROM VEDTAK WHERE STATUS = ?",
                        (rs, rowNum) ->
                                Optional.ofNullable(rs.getTimestamp(dateKolonne))
                                        .map(Timestamp::toLocalDateTime)
                                        .orElse(null),
                        vedtakStatus.name()));
    }

    private void prepareDatabasen(String versjonPrefix) {
        DbTestUtils.testMigrate(db.getDataSource(), versjonPrefix);
    }

    @AfterEach
    public void setupShutdownHook() {
        DbTestUtils.cleanupDb(db);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (postgresContainer != null) {
                postgresContainer.stopContainer();
            }
        }));
    }
}
