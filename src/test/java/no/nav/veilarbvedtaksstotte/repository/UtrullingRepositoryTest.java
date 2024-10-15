package no.nav.veilarbvedtaksstotte.repository;

import no.nav.common.types.identer.EnhetId;
import no.nav.veilarbvedtaksstotte.repository.domain.UtrulletEnhet;
import no.nav.veilarbvedtaksstotte.utils.DatabaseTest;
import no.nav.veilarbvedtaksstotte.utils.DbTestUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UtrullingRepositoryTest extends DatabaseTest {

    private static UtrullingRepository utrullingRepository;
    
    private final EnhetId TEST_ENHET_1_ID = EnhetId.of("1234");
    private final String TEST_ENHET_1_NAVN = "NAV Test";

    private final EnhetId TEST_ENHET_2_ID = EnhetId.of("4321");
    private final String TEST_ENHET_2_NAVN = "NAV Testheim";
    
    @BeforeAll
    public static void init() {
        utrullingRepository = new UtrullingRepository(jdbcTemplate);
    }

    @BeforeEach
    public void setup() {
        DbTestUtils.cleanupDb(jdbcTemplate);
    }

    @Test
    public void skal_legge_til_og_hente_tilgang() {
        utrullingRepository.leggTilUtrulling(TEST_ENHET_1_ID, TEST_ENHET_1_NAVN);

        List<UtrulletEnhet> alleTilganger = utrullingRepository.hentAlleUtrullinger();

        assertFalse(alleTilganger.isEmpty());
        UtrulletEnhet enhet = alleTilganger.get(0);
        assertEquals(TEST_ENHET_1_ID, enhet.getEnhetId());
        assertEquals(TEST_ENHET_1_NAVN, enhet.getNavn());
        assertNotNull(enhet.getCreatedAt());
    }

    @Test
    public void skal_ikke_ha_tilgang_hvis_tilgang_ikke_lagt_til() {
        utrullingRepository.leggTilUtrulling(TEST_ENHET_1_ID, TEST_ENHET_1_NAVN);
        assertFalse(utrullingRepository.erUtrullet(TEST_ENHET_2_ID));
    }

    @Test
    public void skal_returnere_true_hvis_minst_en_enhet_er_utrullet() {
        utrullingRepository.leggTilUtrulling(TEST_ENHET_1_ID, TEST_ENHET_1_NAVN);
        utrullingRepository.leggTilUtrulling(TEST_ENHET_2_ID, TEST_ENHET_2_NAVN);

        assertTrue(utrullingRepository.erMinstEnEnhetUtrullet(List.of(EnhetId.of("6666"), TEST_ENHET_2_ID)));
    }

    @Test
    public void skal_returnere_false_hvis_ingen_enheter_er_utrullet() {
        utrullingRepository.leggTilUtrulling(TEST_ENHET_1_ID, TEST_ENHET_1_NAVN);
        utrullingRepository.leggTilUtrulling(TEST_ENHET_2_ID, TEST_ENHET_2_NAVN);

        assertFalse(utrullingRepository.erMinstEnEnhetUtrullet(List.of(EnhetId.of("6666"), EnhetId.of("5555"))));
    }

    @Test
    public void skal_ikke_kaste_exception_nar_samme_tilgang_blir_laget() {
        utrullingRepository.leggTilUtrulling(TEST_ENHET_1_ID, TEST_ENHET_1_NAVN);
        utrullingRepository.leggTilUtrulling(TEST_ENHET_1_ID, TEST_ENHET_1_NAVN);
    }

    @Test
    public void skal_hente_alle_tilganger() {
        utrullingRepository.leggTilUtrulling(TEST_ENHET_1_ID, TEST_ENHET_1_NAVN);
        utrullingRepository.leggTilUtrulling(TEST_ENHET_2_ID, TEST_ENHET_2_NAVN);

        assertEquals(2, utrullingRepository.hentAlleUtrullinger().size());
    }

    @Test
    public void skal_slette_tilgang() {
        utrullingRepository.leggTilUtrulling(TEST_ENHET_1_ID, TEST_ENHET_1_NAVN);
        utrullingRepository.leggTilUtrulling(TEST_ENHET_2_ID, TEST_ENHET_2_NAVN);
        utrullingRepository.fjernUtrulling(TEST_ENHET_1_ID);

        List<UtrulletEnhet> alleTilganger = utrullingRepository.hentAlleUtrullinger();

        assertEquals(1, alleTilganger.size());
        assertEquals(TEST_ENHET_2_ID, alleTilganger.get(0).getEnhetId());
    }

}
