package no.nav.veilarbvedtaksstotte.repository;

import no.nav.common.types.identer.EnhetId;
import no.nav.veilarbvedtaksstotte.repository.domain.UtrulletEnhet;
import no.nav.veilarbvedtaksstotte.utils.DbTestUtils;
import no.nav.veilarbvedtaksstotte.utils.SingletonPostgresContainer;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.Assert.*;

public class TilgangskontrollRepositoryTest {

    private static JdbcTemplate db;
    private static UtrullingRepository utrullingRepository;

    @BeforeClass
    public static void init() {
        db = SingletonPostgresContainer.init().createJdbcTemplate();
        utrullingRepository = new UtrullingRepository(db);
    }

    @Before
    public void setup() {
        DbTestUtils.cleanupDb(db);
    }

    @Test
    public void skal_legge_til_og_hente_tilgang() {
        utrullingRepository.leggTilUtrulling(EnhetId.of("1234"));

        List<UtrulletEnhet> alleTilganger = utrullingRepository.hentAlleUtrullinger();

        assertFalse(alleTilganger.isEmpty());
        assertEquals(EnhetId.of("1234"), alleTilganger.get(0).getEnhetId());
        assertNotNull(alleTilganger.get(0).getCreatedAt());
    }

    @Test
    public void skal_ikke_ha_tilgang_hvis_tilgang_ikke_lagt_til() {
        utrullingRepository.leggTilUtrulling(EnhetId.of("1234"));
        assertFalse(utrullingRepository.erUtrullet(EnhetId.of("4321")));
    }

    @Test
    public void skal_ikke_kaste_exception_nar_samme_tilgang_blir_laget() {
        utrullingRepository.leggTilUtrulling(EnhetId.of("1234"));
        utrullingRepository.leggTilUtrulling(EnhetId.of("1234"));
    }

    @Test
    public void skal_hente_alle_tilganger() {
        utrullingRepository.leggTilUtrulling(EnhetId.of("1234"));
        utrullingRepository.leggTilUtrulling(EnhetId.of("4321"));

        assertEquals(2, utrullingRepository.hentAlleUtrullinger().size());
    }

    @Test
    public void skal_slette_tilgang() {
        utrullingRepository.leggTilUtrulling(EnhetId.of("1234"));
        utrullingRepository.leggTilUtrulling(EnhetId.of("4321"));
        utrullingRepository.fjernUtrulling(EnhetId.of("1234"));

        List<UtrulletEnhet> alleTilganger = utrullingRepository.hentAlleUtrullinger();

        assertEquals(1, alleTilganger.size());
        assertEquals(EnhetId.of("4321"), alleTilganger.get(0).getEnhetId());
    }

}
