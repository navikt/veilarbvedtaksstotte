package no.nav.veilarbvedtaksstotte.repository;

import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.NavIdent;
import no.nav.veilarbvedtaksstotte.domain.beslutteroversikt.*;
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

        List<BeslutteroversiktBruker> brukere = beslutteroversiktRepository.sokEtterBrukere(sok, NavIdent.of(TEST_BESLUTTER_IDENT)).getBrukere();

        assertEquals(3, brukere.size());
    }

    @Test
    public void sokEtterBrukere__skal_finne_bruker_med_status() {
        BeslutteroversiktSok sok = new BeslutteroversiktSok()
                .withFilter(new BeslutteroversiktSokFilter().withStatus(BeslutteroversiktStatus.KLAR_TIL_BESLUTTER));

        List<BeslutteroversiktBruker> brukere = beslutteroversiktRepository.sokEtterBrukere(sok, NavIdent.of(TEST_BESLUTTER_IDENT)).getBrukere();

        assertEquals(1, brukere.size());
    }

    @Test
    public void sokEtterBrukere__skal_finne_bruker_med_1_enhet() {
        BeslutteroversiktSok sok = new BeslutteroversiktSok()
                .withFilter(new BeslutteroversiktSokFilter().withEnheter(Collections.singletonList(EnhetId.of("1234"))));

        List<BeslutteroversiktBruker> brukere = beslutteroversiktRepository.sokEtterBrukere(sok, NavIdent.of(TEST_BESLUTTER_IDENT)).getBrukere();

        assertEquals(1, brukere.size());
    }

    @Test
    public void sokEtterBrukere__skal_finne_bruker_med_flere_enheter() {
        BeslutteroversiktSok sok = new BeslutteroversiktSok()
                .withFilter(new BeslutteroversiktSokFilter().withEnheter(Arrays.asList(EnhetId.of("1234"), EnhetId.of("6755"))));

        List<BeslutteroversiktBruker> brukere = beslutteroversiktRepository.sokEtterBrukere(sok, NavIdent.of(TEST_BESLUTTER_IDENT)).getBrukere();

        assertEquals(2, brukere.size());
    }

    @Test
    public void sokEtterBrukere__skal_finne_mine_brukere() {
        BeslutteroversiktSok sok = new BeslutteroversiktSok()
                .withFilter(new BeslutteroversiktSokFilter().withVisMineBrukere(true));

        List<BeslutteroversiktBruker> brukere = beslutteroversiktRepository.sokEtterBrukere(sok, NavIdent.of(TEST_BESLUTTER_IDENT)).getBrukere();

        assertEquals(2, brukere.size());
    }

    @Test
    public void sokEtterBrukere__skal_finne_bruker_med_flere_filter() {
        BeslutteroversiktSok sok = new BeslutteroversiktSok()
                .withFilter(
                        new BeslutteroversiktSokFilter()
                                .withEnheter(Arrays.asList(EnhetId.of("1234"), EnhetId.of("6755")))
                                .withStatus(BeslutteroversiktStatus.TRENGER_BESLUTTER)
                );

        List<BeslutteroversiktBruker> brukere = beslutteroversiktRepository.sokEtterBrukere(sok, NavIdent.of(TEST_BESLUTTER_IDENT)).getBrukere();

        assertEquals(1, brukere.size());
    }

    @Test
    public void sokEtterBrukere__skal_sortere_pa_fnr_asc() {
        BeslutteroversiktSok sok = new BeslutteroversiktSok()
                .withOrderByDirection(BeslutteroversiktSok.OrderByDirection.ASC)
                .withOrderByField(BeslutteroversiktSok.OrderByField.BRUKER_FNR);
        List<BeslutteroversiktBruker> brukere = beslutteroversiktRepository.sokEtterBrukere(sok, NavIdent.of(TEST_BESLUTTER_IDENT)).getBrukere();

        assertEquals("011111111111", brukere.getFirst().getBrukerFnr());
    }

    @Test
    public void sokEtterBrukere__skal_sortere_pa_fnr_desc() {
        BeslutteroversiktSok sok = new BeslutteroversiktSok()
                .withOrderByDirection(BeslutteroversiktSok.OrderByDirection.DESC)
                .withOrderByField(BeslutteroversiktSok.OrderByField.BRUKER_FNR);

        List<BeslutteroversiktBruker> brukere = beslutteroversiktRepository.sokEtterBrukere(sok, NavIdent.of(TEST_BESLUTTER_IDENT)).getBrukere();

        assertEquals("9999999999", brukere.getFirst().getBrukerFnr());
    }

    @Test
    public void sokEtterBrukere__skal_begrense_antall() {
        BeslutteroversiktSok sok = new BeslutteroversiktSok()
                .withAntall(1);

        List<BeslutteroversiktBruker> brukere = beslutteroversiktRepository.sokEtterBrukere(sok, NavIdent.of(TEST_BESLUTTER_IDENT)).getBrukere();

        assertEquals(1, brukere.size());
    }

    @Test
    public void sokEtterBrukere__skal_hoppe_over() {
        BeslutteroversiktSok sok = new BeslutteroversiktSok()
                .withFra(1);

        List<BeslutteroversiktBruker> brukere = beslutteroversiktRepository.sokEtterBrukere(sok, NavIdent.of(TEST_BESLUTTER_IDENT)).getBrukere();

        assertEquals("011111111111", brukere.getFirst().getBrukerFnr());
    }

    @Test
    public void sokEtterBrukere__skal_finne_totalt_antall() {
        BeslutteroversiktSok sok = new BeslutteroversiktSok()
                .withAntall(1)
                .withFilter(new BeslutteroversiktSokFilter().withEnheter(Arrays.asList(EnhetId.of("1234"), EnhetId.of("6755"))));

        BrukereMedAntall brukereMedAntall = beslutteroversiktRepository.sokEtterBrukere(sok, NavIdent.of(TEST_BESLUTTER_IDENT));

        assertEquals(1, brukereMedAntall.getBrukere().size());
        assertEquals(2, brukereMedAntall.getTotaltAntall());
    }

}
