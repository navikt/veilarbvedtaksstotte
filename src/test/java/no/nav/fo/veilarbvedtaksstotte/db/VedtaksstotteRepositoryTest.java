package no.nav.fo.veilarbvedtaksstotte.db;

import no.nav.fo.veilarbvedtaksstotte.domain.Vedtak;
import no.nav.fo.veilarbvedtaksstotte.repository.KilderRepository;
import no.nav.fo.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import no.nav.fo.veilarbvedtaksstotte.utils.DbTestUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import org.junit.jupiter.api.AfterEach;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

public class VedtaksstotteRepositoryTest {

    private static DataSource testDataSource = DbTestUtils.createTestDataSource();
    private JdbcTemplate db = new JdbcTemplate(testDataSource);
    private KilderRepository kilderRepositoryMock = mock(KilderRepository.class);

    private final String aktorId = "123";
    private final String veilederIdent = "Z12345";
    private final String veilederEnhetId = "1234";
    private final String veilederEnhetNavn = "NAV Testheim";

    @BeforeClass
    public static void setup() {
        DbTestUtils.testMigrate(testDataSource);
    }

    @AfterEach
    public void cleanup() {
        DbTestUtils.cleanupDb(db);
    }

    @Test
    public void testHentUtkast() {
        VedtaksstotteRepository repo = new VedtaksstotteRepository(db, kilderRepositoryMock);

        repo.insertUtkast(aktorId, veilederIdent, veilederEnhetId, veilederEnhetNavn);

        Vedtak utkast = repo.hentUtkast(aktorId);

        assertEquals(aktorId, utkast.getAktorId());
        assertEquals(veilederIdent, utkast.getVeilederIdent());
        assertEquals(veilederEnhetId, utkast.getVeilederEnhetId());
        assertEquals(veilederEnhetNavn, utkast.getVeilederEnhetNavn());
    }

    @Test
    public void testHentUtkastHvisIkkeFinnes() {
        VedtaksstotteRepository repo = new VedtaksstotteRepository(db, kilderRepositoryMock);
        assertNull(repo.hentUtkast("54385638405"));
    }

    @Test
    public void testHentUtkastMedOpplysninger() {
        // TODO
    }



}
