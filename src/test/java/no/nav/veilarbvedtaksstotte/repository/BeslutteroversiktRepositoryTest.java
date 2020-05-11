package no.nav.veilarbvedtaksstotte.repository;

import no.nav.veilarbvedtaksstotte.domain.BeslutteroversiktBruker;
import no.nav.veilarbvedtaksstotte.domain.enums.BeslutteroversiktStatus;
import no.nav.veilarbvedtaksstotte.utils.DbTestUtils;
import no.nav.veilarbvedtaksstotte.utils.SingletonPostgresContainer;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;

import static no.nav.veilarbvedtaksstotte.utils.TestData.*;
import static org.junit.Assert.*;

public class BeslutteroversiktRepositoryTest {

    private static JdbcTemplate db;
    private static VedtaksstotteRepository vedtaksstotteRepository;
    private static BeslutteroversiktRepository beslutteroversiktRepository;
    private static TransactionTemplate transactor;

    @BeforeClass
    public static void setup() {
        db = SingletonPostgresContainer.init().getDb();
        transactor = new TransactionTemplate(new DataSourceTransactionManager(db.getDataSource()));
        beslutteroversiktRepository = new BeslutteroversiktRepository(db);
        vedtaksstotteRepository = new VedtaksstotteRepository(db, new KilderRepository(db), transactor);
    }

    @Before
    public void cleanup() {
        DbTestUtils.cleanupDb(db);
        vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID);
    }

    @Test
    public void lagBruker__skal_lage_bruker() {
        long vedtakId = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID).getId();

        BeslutteroversiktBruker nyBruker = lagBruker(vedtakId);

        beslutteroversiktRepository.lagBruker(nyBruker);

        BeslutteroversiktBruker bruker = beslutteroversiktRepository.finnBrukerForVedtak(vedtakId)
                .setStatusEndret(null);

        assertEquals(nyBruker, bruker);
    }

    @Test
    public void slettBruke__skal_slette_bruker() {
        long vedtakId = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID).getId();

        BeslutteroversiktBruker bruker = lagBruker(vedtakId);

        beslutteroversiktRepository.lagBruker(bruker);
        beslutteroversiktRepository.slettBruker(vedtakId);

        assertNull(beslutteroversiktRepository.finnBrukerForVedtak(vedtakId));
    }

    @Test
    public void oppdaterStatus__skal_oppdatere_status() {
        long vedtakId = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID).getId();

        BeslutteroversiktBruker nyBruker = lagBruker(vedtakId)
                .setStatus(BeslutteroversiktStatus.TRENGER_BESLUTTER);

        beslutteroversiktRepository.lagBruker(nyBruker);

        nyBruker = beslutteroversiktRepository.finnBrukerForVedtak(vedtakId);

        beslutteroversiktRepository.oppdaterStatus(vedtakId, BeslutteroversiktStatus.KLAR_TIL_VEILEDER);

        BeslutteroversiktBruker oppdatertBruker = beslutteroversiktRepository.finnBrukerForVedtak(vedtakId);

        assertEquals(BeslutteroversiktStatus.KLAR_TIL_VEILEDER, oppdatertBruker.getStatus());
        assertNotEquals(nyBruker.getStatusEndret(), oppdatertBruker.getStatusEndret());
    }

    @Test
    public void oppdaterVeileder__skal_oppdatere_veileder() {
        long vedtakId = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID).getId();

        BeslutteroversiktBruker nyBruker = lagBruker(vedtakId);

        beslutteroversiktRepository.lagBruker(nyBruker);
        beslutteroversiktRepository.oppdaterVeileder(vedtakId, "NY VEILEDER");

        BeslutteroversiktBruker oppdatertBruker = beslutteroversiktRepository.finnBrukerForVedtak(vedtakId);

        assertEquals("NY VEILEDER", oppdatertBruker.getVeilederNavn());
    }

    @Test
    public void oppdaterBeslutter__skal_oppdatere_beslutter() {
        long vedtakId = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID).getId();

        BeslutteroversiktBruker nyBruker = lagBruker(vedtakId);

        beslutteroversiktRepository.lagBruker(nyBruker);
        beslutteroversiktRepository.oppdaterBeslutter(vedtakId, "NY_BESLUTTER_IDENT","NY_BESLUTTER");

        BeslutteroversiktBruker oppdatertBruker = beslutteroversiktRepository.finnBrukerForVedtak(vedtakId);

        assertEquals("NY_BESLUTTER_IDENT", oppdatertBruker.getBeslutterIdent());
        assertEquals("NY_BESLUTTER", oppdatertBruker.getBeslutterNavn());
    }

    private BeslutteroversiktBruker lagBruker(long vedtakId) {
        return new BeslutteroversiktBruker()
                .setVedtakId(vedtakId)
                .setBrukerFornavn("fornavn")
                .setBrukerEtternavn("etternavn")
                .setBrukerOppfolgingsenhetNavn(TEST_OPPFOLGINGSENHET_NAVN)
                .setBrukerOppfolgingsenhetId(TEST_OPPFOLGINGSENHET_ID)
                .setBrukerFnr(TEST_FNR)
                .setVedtakStartet(LocalDateTime.now())
                .setStatus(BeslutteroversiktStatus.KLAR_TIL_BESLUTTER)
                .setBeslutterNavn(TEST_BESLUTTER_NAVN)
                .setBeslutterIdent(TEST_BESLUTTER_IDENT)
                .setVeilederNavn(TEST_VEILEDER_NAVN);
    }

}
