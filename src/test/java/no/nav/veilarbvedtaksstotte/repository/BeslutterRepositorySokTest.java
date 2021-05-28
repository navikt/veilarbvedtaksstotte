package no.nav.veilarbvedtaksstotte.repository;

import no.nav.veilarbvedtaksstotte.domain.beslutteroversikt.*;
import no.nav.veilarbvedtaksstotte.utils.DatabaseTest;
import no.nav.veilarbvedtaksstotte.utils.DbTestUtils;
import no.nav.veilarbvedtaksstotte.utils.TestUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_BESLUTTER_IDENT;
import static org.junit.Assert.assertEquals;

public class BeslutterRepositorySokTest extends DatabaseTest {

    private static BeslutteroversiktRepository beslutteroversiktRepository;

    @BeforeClass
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
        BeslutteroversiktSok sok = new BeslutteroversiktSok()
                .setFilter(new BeslutteroversiktSokFilter().setStatus(BeslutteroversiktStatus.KLAR_TIL_BESLUTTER));

        List<BeslutteroversiktBruker> brukere = beslutteroversiktRepository.sokEtterBrukere(sok, TEST_BESLUTTER_IDENT).getBrukere();

        assertEquals(1, brukere.size());
    }

    @Test
    public void sokEtterBrukere__skal_finne_bruker_med_1_enhet() {
        BeslutteroversiktSok sok = new BeslutteroversiktSok()
                .setFilter(new BeslutteroversiktSokFilter().setEnheter(Collections.singletonList("1234")));

        List<BeslutteroversiktBruker> brukere = beslutteroversiktRepository.sokEtterBrukere(sok, TEST_BESLUTTER_IDENT).getBrukere();

        assertEquals(1, brukere.size());
    }

    @Test
    public void sokEtterBrukere__skal_finne_bruker_med_flere_enheter() {
        BeslutteroversiktSok sok = new BeslutteroversiktSok()
                .setFilter(new BeslutteroversiktSokFilter().setEnheter(Arrays.asList("1234", "6755")));

        List<BeslutteroversiktBruker> brukere = beslutteroversiktRepository.sokEtterBrukere(sok, TEST_BESLUTTER_IDENT).getBrukere();

        assertEquals(2, brukere.size());
    }

    @Test
    public void sokEtterBrukere__skal_finne_bruker_med_fnr() {
        BeslutteroversiktSok sok = new BeslutteroversiktSok()
                .setFilter(new BeslutteroversiktSokFilter().setNavnEllerFnr("123456"));

        List<BeslutteroversiktBruker> brukere = beslutteroversiktRepository.sokEtterBrukere(sok, TEST_BESLUTTER_IDENT).getBrukere();

        assertEquals(1, brukere.size());
    }

    @Test
    public void sokEtterBrukere__skal_finne_bruker_med_delvis_etternavn() {
        BeslutteroversiktSok sok = new BeslutteroversiktSok()
                .setFilter(new BeslutteroversiktSokFilter().setNavnEllerFnr("arls"));

        List<BeslutteroversiktBruker> brukere = beslutteroversiktRepository.sokEtterBrukere(sok, TEST_BESLUTTER_IDENT).getBrukere();

        assertEquals(1, brukere.size());
    }

    @Test
    public void sokEtterBrukere__skal_finne_bruker_med_delvis_fornavn() {
        BeslutteroversiktSok sok = new BeslutteroversiktSok()
                .setFilter(new BeslutteroversiktSokFilter().setNavnEllerFnr("ari"));

        List<BeslutteroversiktBruker> brukere = beslutteroversiktRepository.sokEtterBrukere(sok, TEST_BESLUTTER_IDENT).getBrukere();

        assertEquals(1, brukere.size());
    }

    @Test
    public void sokEtterBrukere__skal_finne_bruker_med_fornavn_og_etternavn() {
        BeslutteroversiktSok sok = new BeslutteroversiktSok()
                .setFilter(new BeslutteroversiktSokFilter().setNavnEllerFnr("Kari Karlsen"));

        List<BeslutteroversiktBruker> brukere = beslutteroversiktRepository.sokEtterBrukere(sok, TEST_BESLUTTER_IDENT).getBrukere();

        assertEquals(1, brukere.size());
    }

    @Test
    public void sokEtterBrukere__skal_finne_mine_brukere() {
        BeslutteroversiktSok sok = new BeslutteroversiktSok()
                .setFilter(new BeslutteroversiktSokFilter().setVisMineBrukere(true));

        List<BeslutteroversiktBruker> brukere = beslutteroversiktRepository.sokEtterBrukere(sok, TEST_BESLUTTER_IDENT).getBrukere();

        assertEquals(2, brukere.size());
    }

    @Test
    public void sokEtterBrukere__skal_finne_bruker_med_flere_filter() {
        BeslutteroversiktSok sok = new BeslutteroversiktSok()
                .setFilter(
                        new BeslutteroversiktSokFilter()
                                .setEnheter(Arrays.asList("1234", "6755"))
                                .setStatus(BeslutteroversiktStatus.TRENGER_BESLUTTER)
                );

        List<BeslutteroversiktBruker> brukere = beslutteroversiktRepository.sokEtterBrukere(sok, TEST_BESLUTTER_IDENT).getBrukere();

        assertEquals(1, brukere.size());
    }

    @Test
    public void sokEtterBrukere__skal_sortere_pa_fnr_asc() {
        BeslutteroversiktSok sok = new BeslutteroversiktSok()
                .setOrderByField(BeslutteroversiktSok.OrderByField.BRUKER_FNR)
                .setOrderByDirection(BeslutteroversiktSok.OrderByDirection.ASC);

        List<BeslutteroversiktBruker> brukere = beslutteroversiktRepository.sokEtterBrukere(sok, TEST_BESLUTTER_IDENT).getBrukere();

        assertEquals("011111111111",  brukere.get(0).getBrukerFnr());
    }

    @Test
    public void sokEtterBrukere__skal_sortere_pa_fnr_desc() {
        BeslutteroversiktSok sok = new BeslutteroversiktSok()
                .setOrderByField(BeslutteroversiktSok.OrderByField.BRUKER_FNR)
                .setOrderByDirection(BeslutteroversiktSok.OrderByDirection.DESC);

        List<BeslutteroversiktBruker> brukere = beslutteroversiktRepository.sokEtterBrukere(sok, TEST_BESLUTTER_IDENT).getBrukere();

        assertEquals("9999999999",  brukere.get(0).getBrukerFnr());
    }

    @Test
    public void sokEtterBrukere__skal_begrense_antall() {
        BeslutteroversiktSok sok = new BeslutteroversiktSok()
                .setAntall(1);

        List<BeslutteroversiktBruker> brukere = beslutteroversiktRepository.sokEtterBrukere(sok, TEST_BESLUTTER_IDENT).getBrukere();

        assertEquals(1, brukere.size());
    }

    @Test
    public void sokEtterBrukere__skal_hoppe_over() {
        BeslutteroversiktSok sok = new BeslutteroversiktSok()
                .setFra(1);

        List<BeslutteroversiktBruker> brukere = beslutteroversiktRepository.sokEtterBrukere(sok, TEST_BESLUTTER_IDENT).getBrukere();

        assertEquals("011111111111", brukere.get(0).getBrukerFnr());
    }

    @Test
    public void sokEtterBrukere__skal_finne_totalt_antall() {
        BeslutteroversiktSok sok = new BeslutteroversiktSok()
                .setAntall(1)
                .setFilter(new BeslutteroversiktSokFilter().setEnheter(Arrays.asList("1234", "6755")));

        BrukereMedAntall brukereMedAntall = beslutteroversiktRepository.sokEtterBrukere(sok, TEST_BESLUTTER_IDENT);

        assertEquals(1, brukereMedAntall.getBrukere().size());
        assertEquals(2, brukereMedAntall.getTotaltAntall());
    }

}
