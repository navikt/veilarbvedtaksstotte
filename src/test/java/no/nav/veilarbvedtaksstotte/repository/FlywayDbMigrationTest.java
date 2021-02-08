package no.nav.veilarbvedtaksstotte.repository;

import static no.nav.veilarbvedtaksstotte.utils.TestData.*;

import no.nav.veilarbvedtaksstotte.utils.SingletonPostgresContainer;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

public class FlywayDbMigrationTest {
    private static JdbcTemplate db;
    private static TransactionTemplate transactor;
    private static KilderRepository kilderRepository;
    private static VedtaksstotteRepository vedtaksstotteRepository;

    @Test
    public void testBeforeVersion23() {
        final String versionPrefix = "V22";
        db = SingletonPostgresContainer.init(versionPrefix).getDb();
        transactor = new TransactionTemplate(new DataSourceTransactionManager(db.getDataSource()));
        kilderRepository = new KilderRepository(db);
        vedtaksstotteRepository = new VedtaksstotteRepository(db, transactor);

        vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID);

    }
}
