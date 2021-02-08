package no.nav.veilarbvedtaksstotte.repository;

import static no.nav.veilarbvedtaksstotte.utils.TestData.*;
import static org.junit.Assert.assertNotNull;

import no.nav.veilarbvedtaksstotte.client.dokument.DokumentSendtDTO;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak;
import no.nav.veilarbvedtaksstotte.utils.SingletonPostgresContainer;
import no.nav.veilarbvedtaksstotte.utils.TestData;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

public class FlywayDbMigrationTest {
    private static JdbcTemplate db;
    private static TransactionTemplate transactor;
    private static KilderRepository kilderRepository;
    private static VedtaksstotteRepository vedtaksstotteRepository;

    @Test(expected = Exception.class)
    public void testBeforeVersion23() {
        final String versionPrefix = "22";
        db = SingletonPostgresContainer.init(versionPrefix).getDb();
        transactor = new TransactionTemplate(new DataSourceTransactionManager(db.getDataSource()));
        kilderRepository = new KilderRepository(db);

        vedtaksstotteRepository = new VedtaksstotteRepository(db, transactor);
        vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID);
        vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);
    }

    @Test
    public void testVersion23() {

        db = SingletonPostgresContainer.init().getDb();
        transactor = new TransactionTemplate(new DataSourceTransactionManager(db.getDataSource()));
        kilderRepository = new KilderRepository(db);

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
}
