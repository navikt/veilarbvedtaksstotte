package no.nav.veilarbvedtaksstotte.repository;

import no.nav.common.types.identer.EnhetId;
import no.nav.veilarbvedtaksstotte.repository.domain.EnhetTilgang;
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
    private static TilgangskontrollRepository tilgangskontrollRepository;

    @BeforeClass
    public static void init() {
        db = SingletonPostgresContainer.init().createJdbcTemplate();
        tilgangskontrollRepository = new TilgangskontrollRepository(db);
    }

    @Before
    public void setup() {
        DbTestUtils.cleanupDb(db);
    }

    @Test
    public void skal_legge_til_og_hente_tilgang() {
        tilgangskontrollRepository.leggTilTilgang(EnhetId.of("1234"));

        List<EnhetTilgang> alleTilganger = tilgangskontrollRepository.hentAlleTilganger();

        assertFalse(alleTilganger.isEmpty());
        assertEquals(EnhetId.of("1234"), alleTilganger.get(0).getEnhetId());
        assertNotNull(alleTilganger.get(0).getCreatedAt());
    }

    @Test
    public void skal_ikke_ha_tilgang_hvis_tilgang_ikke_lagt_til() {
        tilgangskontrollRepository.leggTilTilgang(EnhetId.of("1234"));
        assertFalse(tilgangskontrollRepository.harEnhetTilgang(EnhetId.of("4321")));
    }

    @Test
    public void skal_ikke_kaste_exception_nar_samme_tilgang_blir_laget() {
        tilgangskontrollRepository.leggTilTilgang(EnhetId.of("1234"));
        tilgangskontrollRepository.leggTilTilgang(EnhetId.of("1234"));
    }

    @Test
    public void skal_hente_alle_tilganger() {
        tilgangskontrollRepository.leggTilTilgang(EnhetId.of("1234"));
        tilgangskontrollRepository.leggTilTilgang(EnhetId.of("4321"));

        assertEquals(2, tilgangskontrollRepository.hentAlleTilganger().size());
    }

    @Test
    public void skal_slette_tilgang() {
        tilgangskontrollRepository.leggTilTilgang(EnhetId.of("1234"));
        tilgangskontrollRepository.leggTilTilgang(EnhetId.of("4321"));
        tilgangskontrollRepository.fjernTilgang(EnhetId.of("1234"));

        List<EnhetTilgang> alleTilganger = tilgangskontrollRepository.hentAlleTilganger();

        assertEquals(1, alleTilganger.size());
        assertEquals(EnhetId.of("4321"), alleTilganger.get(0).getEnhetId());
    }

}
