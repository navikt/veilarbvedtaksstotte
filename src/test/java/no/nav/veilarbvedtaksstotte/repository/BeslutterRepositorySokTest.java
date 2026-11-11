package no.nav.veilarbvedtaksstotte.repository;

import no.nav.veilarbvedtaksstotte.domain.beslutteroversikt.BeslutteroversiktBruker;
import no.nav.veilarbvedtaksstotte.domain.beslutteroversikt.BeslutteroversiktSok;
import no.nav.veilarbvedtaksstotte.domain.beslutteroversikt.BeslutteroversiktSokFilter;
import no.nav.veilarbvedtaksstotte.domain.beslutteroversikt.BeslutteroversiktStatus;
import no.nav.veilarbvedtaksstotte.domain.beslutteroversikt.BrukereMedAntall;
import no.nav.veilarbvedtaksstotte.utils.DatabaseTest;
import no.nav.veilarbvedtaksstotte.utils.DbTestUtils;
import no.nav.veilarbvedtaksstotte.utils.TestUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_BESLUTTER_IDENT;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class BeslutterRepositorySokTest extends DatabaseTest {

    private static BeslutteroversiktRepository beslutteroversiktRepository;

    @BeforeAll
    public static void setup() {
        beslutteroversiktRepository = new BeslutteroversiktRepository(jdbcTemplate);

        DbTestUtils.cleanupDb(jdbcTemplate);
        String beslutteroversiktBrukereSql = TestUtils.readTestResourceFile("beslutteroversikt-brukere.sql");
        jdbcTemplate.execute(beslutteroversiktBrukereSql);
    }

    @Test
    public void sokEtterBrukere__skal_finne_alle_brukere_uten_filter() {
        BeslutteroversiktSok sok = new BeslutteroversiktSok();

        List<BeslutteroversiktBruker> brukere = beslutteroversiktRepository.sokEtterBrukere(sok, TEST_BESLUTTER_IDENT).getBrukere();

        assertEquals(3, brukere.size());
    }

    @Test
    public void sokEtterBrukere__skal_finne_bruker_med_status() {
        BeslutteroversiktSokFilter filter = new BeslutteroversiktSokFilter();
        filter.setStatus(BeslutteroversiktStatus.KLAR_TIL_BESLUTTER);
        BeslutteroversiktSok sok = new BeslutteroversiktSok();
        sok.setFilter(filter);

        List<BeslutteroversiktBruker> brukere = beslutteroversiktRepository.sokEtterBrukere(sok, TEST_BESLUTTER_IDENT).getBrukere();

        assertEquals(1, brukere.size());
    }

    @Test
    public void sokEtterBrukere__skal_finne_bruker_med_1_enhet() {
        BeslutteroversiktSokFilter filter = new BeslutteroversiktSokFilter();
        filter.setEnheter(Collections.singletonList("1234"));
        BeslutteroversiktSok sok = new BeslutteroversiktSok();
        sok.setFilter(filter);

        List<BeslutteroversiktBruker> brukere = beslutteroversiktRepository.sokEtterBrukere(sok, TEST_BESLUTTER_IDENT).getBrukere();

        assertEquals(1, brukere.size());
    }

    @Test
    public void sokEtterBrukere__skal_finne_bruker_med_flere_enheter() {
        BeslutteroversiktSokFilter filter = new BeslutteroversiktSokFilter();
        filter.setEnheter(Arrays.asList("1234", "6755"));
        BeslutteroversiktSok sok = new BeslutteroversiktSok();
        sok.setFilter(filter);

        List<BeslutteroversiktBruker> brukere = beslutteroversiktRepository.sokEtterBrukere(sok, TEST_BESLUTTER_IDENT).getBrukere();

        assertEquals(2, brukere.size());
    }

    @Test
    public void sokEtterBrukere__skal_finne_mine_brukere() {
        BeslutteroversiktSokFilter filter = new BeslutteroversiktSokFilter();
        filter.setVisMineBrukere(true);
        BeslutteroversiktSok sok = new BeslutteroversiktSok();
        sok.setFilter(filter);

        List<BeslutteroversiktBruker> brukere = beslutteroversiktRepository.sokEtterBrukere(sok, TEST_BESLUTTER_IDENT).getBrukere();

        assertEquals(2, brukere.size());
    }

    @Test
    public void sokEtterBrukere__skal_finne_bruker_med_flere_filter() {
        BeslutteroversiktSokFilter filter = new BeslutteroversiktSokFilter();
        filter.setEnheter(Arrays.asList("1234", "6755"));
        filter.setStatus(BeslutteroversiktStatus.TRENGER_BESLUTTER);
        BeslutteroversiktSok sok = new BeslutteroversiktSok();
        sok.setFilter(filter);

        List<BeslutteroversiktBruker> brukere = beslutteroversiktRepository.sokEtterBrukere(sok, TEST_BESLUTTER_IDENT).getBrukere();

        assertEquals(1, brukere.size());
    }

    @Test
    public void sokEtterBrukere__skal_sortere_pa_fnr_asc() {
        BeslutteroversiktSok sok = new BeslutteroversiktSok();
        sok.setOrderByDirection(BeslutteroversiktSok.OrderByDirection.ASC);
        sok.setOrderByField(BeslutteroversiktSok.OrderByField.BRUKER_FNR);
        List<BeslutteroversiktBruker> brukere = beslutteroversiktRepository.sokEtterBrukere(sok, TEST_BESLUTTER_IDENT).getBrukere();

        assertEquals("011111111111",  brukere.getFirst().getBrukerFnr());
    }

    @Test
    public void sokEtterBrukere__skal_sortere_pa_fnr_desc() {
        BeslutteroversiktSok sok = new BeslutteroversiktSok();
        sok.setOrderByDirection(BeslutteroversiktSok.OrderByDirection.DESC);
        sok.setOrderByField(BeslutteroversiktSok.OrderByField.BRUKER_FNR);

        List<BeslutteroversiktBruker> brukere = beslutteroversiktRepository.sokEtterBrukere(sok, TEST_BESLUTTER_IDENT).getBrukere();

        assertEquals("9999999999",  brukere.getFirst().getBrukerFnr());
    }

    @Test
    public void sokEtterBrukere__skal_begrense_antall() {
        BeslutteroversiktSok sok = new BeslutteroversiktSok();
        sok.setAntall(1);

        List<BeslutteroversiktBruker> brukere = beslutteroversiktRepository.sokEtterBrukere(sok, TEST_BESLUTTER_IDENT).getBrukere();

        assertEquals(1, brukere.size());
    }

    @Test
    public void sokEtterBrukere__skal_hoppe_over() {
        BeslutteroversiktSok sok = new BeslutteroversiktSok();
        sok.setFra(1);

        List<BeslutteroversiktBruker> brukere = beslutteroversiktRepository.sokEtterBrukere(sok, TEST_BESLUTTER_IDENT).getBrukere();

        assertEquals("011111111111", brukere.getFirst().getBrukerFnr());
    }

    @Test
    public void sokEtterBrukere__skal_finne_totalt_antall() {
        BeslutteroversiktSokFilter filter = new BeslutteroversiktSokFilter();
        filter.setEnheter(Arrays.asList("1234", "6755"));
        BeslutteroversiktSok sok = new BeslutteroversiktSok();
        sok.setAntall(1);
        sok.setFilter(filter);

        BrukereMedAntall brukereMedAntall = beslutteroversiktRepository.sokEtterBrukere(sok, TEST_BESLUTTER_IDENT);

        assertEquals(1, brukereMedAntall.getBrukere().size());
        assertEquals(2, brukereMedAntall.getTotaltAntall());
    }

}
