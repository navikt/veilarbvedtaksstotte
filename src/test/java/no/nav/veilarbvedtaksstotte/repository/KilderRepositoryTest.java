package no.nav.veilarbvedtaksstotte.repository;

import no.nav.veilarbvedtaksstotte.domain.vedtak.KildeForVedtak;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak;
import no.nav.veilarbvedtaksstotte.utils.DatabaseTest;
import no.nav.veilarbvedtaksstotte.utils.DbTestUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Arrays;
import java.util.List;

import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_AKTOR_ID;
import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_OPPFOLGINGSENHET_ID;
import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_VEILEDER_IDENT;
import static no.nav.veilarbvedtaksstotte.utils.TestData.VEDTAK_ID_THAT_DOES_NOT_EXIST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class KilderRepositoryTest extends DatabaseTest {

    private static KilderRepository kilderRepository;
    private static VedtaksstotteRepository vedtaksstotteRepository;

    @BeforeAll
    public static void setup() {
        kilderRepository = new KilderRepository(jdbcTemplate);
        vedtaksstotteRepository = new VedtaksstotteRepository(jdbcTemplate, transactor);
    }

    @BeforeEach
    public void cleanup() {
        DbTestUtils.cleanupDb(jdbcTemplate);
    }

    @Test
    public void skal_feile_hvis_vedtak_ikke_finnes() {
        List<String> kilder = Arrays.asList("kilde1", "kilde2");

        assertThrows(DataIntegrityViolationException.class, () -> {
            kilderRepository.lagKilder(kilder, VEDTAK_ID_THAT_DOES_NOT_EXIST);
        });
    }

    @Test
    public void skal_lage_og_hente_kilder() {

        vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID);

        long vedtakId = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID).getId();

        List<String> kilder = Arrays.asList("kilde1", "kilde2");
        kilderRepository.lagKilder(kilder, vedtakId);
        List<KildeForVedtak> kilderHentet = kilderRepository.hentKilderForVedtak(vedtakId);

        kilder.forEach(kilde -> {
            assertTrue(kilderHentet.stream().anyMatch(k -> kilde.equals(k.getKilde().getTekst()) && k.getVedtakId() == vedtakId));
        });
    }

    @Test
    public void hentKilderForAlleVedtak__skal_hente_alle_kilder() {
        vedtaksstotteRepository.opprettUtkast("aktor1", TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID);
        vedtaksstotteRepository.opprettUtkast("aktor2", TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID);

        Vedtak vedtak1 = vedtaksstotteRepository.hentUtkast("aktor1");
        Vedtak vedtak2 = vedtaksstotteRepository.hentUtkast("aktor2");

        List<String> kilder = Arrays.asList("kilde1", "kilde2");

        kilderRepository.lagKilder(kilder, vedtak1.getId());
        kilderRepository.lagKilder(kilder, vedtak2.getId());

        List<KildeForVedtak> kilderHentet = kilderRepository.hentKilderForAlleVedtak(List.of(vedtak1, vedtak2));

        assertEquals(4, kilderHentet.size());
    }

}
