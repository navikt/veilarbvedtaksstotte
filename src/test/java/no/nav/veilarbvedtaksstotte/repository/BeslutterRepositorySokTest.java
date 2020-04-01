package no.nav.veilarbvedtaksstotte.repository;

import no.nav.veilarbvedtaksstotte.domain.BeslutteroversiktBruker;
import no.nav.veilarbvedtaksstotte.domain.BeslutteroversiktSok;
import no.nav.veilarbvedtaksstotte.domain.BeslutteroversiktSokFilter;
import no.nav.veilarbvedtaksstotte.domain.enums.BeslutteroversiktStatus;
import no.nav.veilarbvedtaksstotte.utils.DbTestUtils;
import no.nav.veilarbvedtaksstotte.utils.SingletonPostgresContainer;
import no.nav.veilarbvedtaksstotte.utils.TestUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class BeslutterRepositorySokTest {

    private static JdbcTemplate db;
    private static BeslutteroversiktRepository beslutteroversiktRepository;

    @BeforeClass
    public static void setup() {
        db = SingletonPostgresContainer.init().getDb();
        beslutteroversiktRepository = new BeslutteroversiktRepository(new NamedParameterJdbcTemplate(db));

        DbTestUtils.cleanupDb(db);
        String beslutteroversiktBrukereSql = TestUtils.readTestResourceFile("beslutteroversikt-brukere.sql");
        db.execute(beslutteroversiktBrukereSql);
    }

    @Test
    public void sokEtterBrukere__skal_finne_alle_brukere_uten_filter() {
        BeslutteroversiktSok sok = new BeslutteroversiktSok()
                .setAntall(1000);

        List<BeslutteroversiktBruker> brukere = beslutteroversiktRepository.sokEtterBrukere(sok);

        assertEquals(3, brukere.size());
    }

    @Test
    public void sokEtterBrukere__skal_finne_bruker_med_status() {
        BeslutteroversiktSok sok = new BeslutteroversiktSok()
                .setFilter(new BeslutteroversiktSokFilter().setStatus(BeslutteroversiktStatus.HAR_BESLUTTER));

        List<BeslutteroversiktBruker> brukere = beslutteroversiktRepository.sokEtterBrukere(sok);

        assertEquals(1, brukere.size());
    }

    @Test
    public void sokEtterBrukere__skal_finne_bruker_med_1_enhet() {
        BeslutteroversiktSok sok = new BeslutteroversiktSok()
                .setFilter(new BeslutteroversiktSokFilter().setEnheter(Collections.singletonList("1234")));

        List<BeslutteroversiktBruker> brukere = beslutteroversiktRepository.sokEtterBrukere(sok);

        assertEquals(1, brukere.size());
    }

    @Test
    public void sokEtterBrukere__skal_finne_bruker_med_flere_enheter() {
        BeslutteroversiktSok sok = new BeslutteroversiktSok()
                .setFilter(new BeslutteroversiktSokFilter().setEnheter(List.of("1234", "6755")));

        List<BeslutteroversiktBruker> brukere = beslutteroversiktRepository.sokEtterBrukere(sok);

        assertEquals(2, brukere.size());
    }

}
