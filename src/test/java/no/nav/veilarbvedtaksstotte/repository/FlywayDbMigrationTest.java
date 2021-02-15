package no.nav.veilarbvedtaksstotte.repository;

import static no.nav.veilarbvedtaksstotte.utils.TestData.*;
import static org.junit.Assert.assertNotNull;

import no.nav.veilarbvedtaksstotte.client.dokument.DokumentSendtDTO;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak;
import no.nav.veilarbvedtaksstotte.utils.DbTestUtils;
import no.nav.veilarbvedtaksstotte.utils.PostgresContainer;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;


public class FlywayDbMigrationTest {
    private static VedtaksstotteRepository vedtaksstotteRepository;
    private JdbcTemplate db;
    private TransactionTemplate transactor;
    private PostgresContainer postgresContainer;

    @Before
    public void steUp() {
        postgresContainer = new PostgresContainer();
    }


    @Test(expected = Exception.class)
    public void testVersion_before_column_changes_for_v23() {
        final String versjonPrefix = "22";
        prepareDatabasen(versjonPrefix);

        vedtaksstotteRepository = new VedtaksstotteRepository(db, transactor);
        vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID);
        vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);

    }

    @Test
    public void testVersion_after_column_changes_for_v23() {
        final String versionPrefix = "23";
        prepareDatabasen(versionPrefix);

        DokumentSendtDTO dokumentSendtDTO = new DokumentSendtDTO(TEST_JOURNALPOST_ID, TEST_DOKUMENT_ID);

        vedtaksstotteRepository = new VedtaksstotteRepository(db, transactor);
        vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID);
        Vedtak vedtak = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);
        vedtaksstotteRepository.ferdigstillVedtak(vedtak.getId(), dokumentSendtDTO);
        vedtak = vedtaksstotteRepository.hentVedtak(vedtak.getId());


        assertNotNull("Vedtak er null", vedtak);
        assertNotNull(vedtak.getUtkastSistOppdatert());
        assertNotNull("Vedtak fattet dato er null", vedtak.getVedtakFattet());
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
        transactor = new TransactionTemplate(new DataSourceTransactionManager(db.getDataSource()));

        DbTestUtils.testMigrate(postgresContainer.getDataSource(), versjonPrefix);
    }
}
