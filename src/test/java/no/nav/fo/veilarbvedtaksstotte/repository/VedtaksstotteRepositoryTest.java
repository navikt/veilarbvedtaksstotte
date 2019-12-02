package no.nav.fo.veilarbvedtaksstotte.repository;

import no.nav.fo.veilarbvedtaksstotte.domain.Vedtak;
import no.nav.fo.veilarbvedtaksstotte.utils.DbTestUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import org.junit.jupiter.api.AfterEach;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import static no.nav.fo.veilarbvedtaksstotte.repository.TestData.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

public class VedtaksstotteRepositoryTest {

    private static DataSource testDataSource = DbTestUtils.createTestDataSource();
    private JdbcTemplate db = new JdbcTemplate(testDataSource);
    private KilderRepository kilderRepositoryMock = mock(KilderRepository.class);

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

        repo.opprettUtakst(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_VEILEDER_ENHET_ID, TEST_VEILEDER_ENHET_NAVN);

        Vedtak utkast = repo.hentUtkast(TEST_AKTOR_ID);

        assertEquals(TEST_AKTOR_ID, utkast.getAktorId());
        assertEquals(TEST_VEILEDER_IDENT, utkast.getVeilederIdent());
        assertEquals(TEST_VEILEDER_ENHET_ID, utkast.getVeilederEnhetId());
        assertEquals(TEST_VEILEDER_ENHET_NAVN, utkast.getVeilederEnhetNavn());
    }

    @Test
    public void testHentUtkastHvisIkkeFinnes() {
        VedtaksstotteRepository repo = new VedtaksstotteRepository(db, kilderRepositoryMock);
        assertNull(repo.hentUtkast("54385638405"));
    }

//    @Test
//    public void testHentUtkastMedOpplysninger() {
//        // TODO
//    }

}
