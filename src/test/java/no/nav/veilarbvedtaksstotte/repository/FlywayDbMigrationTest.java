package no.nav.veilarbvedtaksstotte.repository;

import static java.lang.String.format;
import static no.nav.veilarbvedtaksstotte.utils.TestData.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import no.nav.veilarbvedtaksstotte.client.dokument.DokumentSendtDTO;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak;
import no.nav.veilarbvedtaksstotte.domain.vedtak.VedtakStatus;
import no.nav.veilarbvedtaksstotte.utils.DbTestUtils;
import no.nav.veilarbvedtaksstotte.utils.PostgresContainer;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Date;


public class FlywayDbMigrationTest {
    private JdbcTemplate db;
    private TransactionTemplate transactor;
    private PostgresContainer postgresContainer;

    @Before
    public void steUp() {
        postgresContainer = new PostgresContainer();
    }


    @Test(expected = Exception.class)
    public void testVersion_for_og_etter_colon_endringene_til_v23() {
        final String versjonPrefix = "22";
        final String versjonEtterVedtakOppdatert = "23";
        prepareDatabasen(versjonPrefix);

        LocalDateTime todaysDate = LocalDateTime.now();

        String sql = format(
                "INSERT INTO %s(%s, %s, %s, %s, %s) values(?, ?, ?, ?, CURRENT_TIMESTAMP)",
                "VEDTAK", TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID, VedtakStatus.UTKAST, todaysDate
        );
        db.execute(sql);

        prepareDatabasen(versjonEtterVedtakOppdatert);


        VedtaksstotteRepository vedtaksstotteRepository = new VedtaksstotteRepository(db, transactor);
        Vedtak utkastVedtak = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);

        assertNotNull("Vedtak er null", utkastVedtak);
        assertEquals(todaysDate, utkastVedtak.getUtkastSistOppdatert());
        assertNotNull("Vedtak fattet dato er null", utkastVedtak.getVedtakFattet());

        DokumentSendtDTO dokumentSendtDTO = new DokumentSendtDTO(TEST_JOURNALPOST_ID, TEST_DOKUMENT_ID);
        vedtaksstotteRepository.ferdigstillVedtak(utkastVedtak.getId(), dokumentSendtDTO);
        Vedtak vedtakFattet = vedtaksstotteRepository.hentVedtak(utkastVedtak.getId());

        assertNotNull("Vedtak er null", vedtakFattet);
        assertEquals(todaysDate, vedtakFattet.getUtkastSistOppdatert());
        assertNotNull("Vedtak fattet dato er null", utkastVedtak.getVedtakFattet());

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

    private void prepareDatabasen(String versjonPrefix) {
        db = postgresContainer.getDb();

        boolean isDbNull = db != null && db.getDataSource() != null;
        if (isDbNull) {
            transactor = new TransactionTemplate(new DataSourceTransactionManager(db.getDataSource()));
        }

        DbTestUtils.testMigrate(postgresContainer.getDataSource(), versjonPrefix);
    }
}
