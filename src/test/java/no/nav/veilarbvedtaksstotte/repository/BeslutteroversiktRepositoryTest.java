package no.nav.veilarbvedtaksstotte.repository;

import no.nav.veilarbvedtaksstotte.domain.beslutteroversikt.BeslutteroversiktBruker;
import no.nav.veilarbvedtaksstotte.domain.beslutteroversikt.BeslutteroversiktStatus;
import no.nav.veilarbvedtaksstotte.domain.beslutteroversikt.EnhetMedNavn;
import no.nav.veilarbvedtaksstotte.utils.DatabaseTest;
import no.nav.veilarbvedtaksstotte.utils.DbTestUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_AKTOR_ID;
import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_BESLUTTER_IDENT;
import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_BESLUTTER_NAVN;
import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_FNR;
import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_OPPFOLGINGSENHET_ID;
import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_OPPFOLGINGSENHET_NAVN;
import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_VEILEDER_IDENT;
import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_VEILEDER_NAVN;
import static no.nav.veilarbvedtaksstotte.utils.TimeUtils.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static java.util.List.of;

public class BeslutteroversiktRepositoryTest extends DatabaseTest {

    private static VedtaksstotteRepository vedtaksstotteRepository;
    private static BeslutteroversiktRepository beslutteroversiktRepository;

    @BeforeAll
    public static void setup() {
        beslutteroversiktRepository = new BeslutteroversiktRepository(jdbcTemplate);
        vedtaksstotteRepository = new VedtaksstotteRepository(jdbcTemplate, transactor);
    }

    @BeforeEach
    public void cleanup() {
        DbTestUtils.cleanupDb(jdbcTemplate);
        vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID);
    }

    @Test
    public void lagBruker__skal_lage_bruker() {
        long vedtakId = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID).getId();

        BeslutteroversiktBruker nyBruker = lagBruker(vedtakId);

        beslutteroversiktRepository.lagBruker(nyBruker);

        BeslutteroversiktBruker bruker = beslutteroversiktRepository.finnBrukerForVedtak(vedtakId)
                .withStatusEndret(null);

        assertEquals(nyBruker, bruker);
    }

    @Test
    public void slettBruke__skal_slette_bruker() {
        long vedtakId = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID).getId();

        BeslutteroversiktBruker bruker = lagBruker(vedtakId);

        beslutteroversiktRepository.lagBruker(bruker);
        beslutteroversiktRepository.slettBruker(vedtakId);

        assertNull(beslutteroversiktRepository.finnBrukerForVedtak(vedtakId));
    }

    @Test
    public void oppdaterStatus__skal_oppdatere_status() {
        long vedtakId = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID).getId();

        BeslutteroversiktBruker nyBruker = lagBruker(vedtakId)
                .withStatus(BeslutteroversiktStatus.TRENGER_BESLUTTER);

        beslutteroversiktRepository.lagBruker(nyBruker);

        nyBruker = beslutteroversiktRepository.finnBrukerForVedtak(vedtakId);

        beslutteroversiktRepository.oppdaterStatus(vedtakId, BeslutteroversiktStatus.KLAR_TIL_VEILEDER);

        BeslutteroversiktBruker oppdatertBruker = beslutteroversiktRepository.finnBrukerForVedtak(vedtakId);

        assertEquals(BeslutteroversiktStatus.KLAR_TIL_VEILEDER, oppdatertBruker.getStatus());
        assertNotEquals(nyBruker.getStatusEndret(), oppdatertBruker.getStatusEndret());
    }

    @Test
    public void oppdaterVeileder__skal_oppdatere_veileder() {
        long vedtakId = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID).getId();

        BeslutteroversiktBruker nyBruker = lagBruker(vedtakId);

        beslutteroversiktRepository.lagBruker(nyBruker);
        beslutteroversiktRepository.oppdaterVeileder(vedtakId, "NY VEILEDER");

        BeslutteroversiktBruker oppdatertBruker = beslutteroversiktRepository.finnBrukerForVedtak(vedtakId);

        assertEquals("NY VEILEDER", oppdatertBruker.getVeilederNavn());
    }

    @Test
    public void oppdaterBeslutter__skal_oppdatere_beslutter() {
        long vedtakId = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID).getId();

        BeslutteroversiktBruker nyBruker = lagBruker(vedtakId);

        beslutteroversiktRepository.lagBruker(nyBruker);
        beslutteroversiktRepository.oppdaterBeslutter(vedtakId, "NY_BESLUTTER_IDENT","NY_BESLUTTER");

        BeslutteroversiktBruker oppdatertBruker = beslutteroversiktRepository.finnBrukerForVedtak(vedtakId);

        assertEquals("NY_BESLUTTER_IDENT", oppdatertBruker.getBeslutterIdent());
        assertEquals("NY_BESLUTTER", oppdatertBruker.getBeslutterNavn());
    }

    @Test
    public void oppdaterBrukerEnhet__skal_oppdatere_enhet() {
        long vedtakId = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID).getId();

        String nyEnhetId = "8888";
        String nyEnhetNavn = "Nav Ny enhet";

        BeslutteroversiktBruker nyBruker = lagBruker(vedtakId);

        beslutteroversiktRepository.lagBruker(nyBruker);
        beslutteroversiktRepository.oppdaterBrukerEnhet(vedtakId, nyEnhetId, nyEnhetNavn);

        BeslutteroversiktBruker oppdatertBruker = beslutteroversiktRepository.finnBrukerForVedtak(vedtakId);

        assertEquals(nyEnhetId, oppdatertBruker.getBrukerOppfolgingsenhetId());
        assertEquals(nyEnhetNavn, oppdatertBruker.getBrukerOppfolgingsenhetNavn());
    }

    @Test
    public void hentEnheterMedUtkast__skal_returnere_enhet_med_navn() {
        long vedtakId = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID).getId();
        String enhetId = "8888";
        String enhetNavn = "Nav Ny enhet";

        beslutteroversiktRepository.lagBruker(lagBruker(vedtakId)
                .withBrukerOppfolgingsenhetId(enhetId)
                .withBrukerOppfolgingsenhetNavn(enhetNavn)
                .withStatus(BeslutteroversiktStatus.TRENGER_BESLUTTER));

        assertEquals(of(new EnhetMedNavn(enhetId, enhetNavn)), beslutteroversiktRepository.hentEnheterMedUtkast());
    }

    @Test
    public void hentEnheterMedUtkast__skal_velge_nyeste_rad_per_enhet_og_sortere_enheter() {
        LocalDateTime eldreVedtak = LocalDateTime.of(2024, 1, 1, 12, 0);
        LocalDateTime nyesteVedtak = LocalDateTime.of(2024, 2, 1, 12, 0);
        LocalDateTime nyesteIkkeAktiveVedtak = LocalDateTime.of(2024, 3, 1, 12, 0);

        beslutteroversiktRepository.lagBruker(lagBruker(opprettVedtakForTest("eldre"))
                .withBrukerOppfolgingsenhetId("9902")
                .withBrukerOppfolgingsenhetNavn("Eldre navn")
                .withVedtakStartet(eldreVedtak));
        beslutteroversiktRepository.lagBruker(lagBruker(opprettVedtakForTest("lik-dato-1"))
                .withBrukerOppfolgingsenhetId("9902")
                .withBrukerOppfolgingsenhetNavn("Lik dato, lavere rad-ID")
                .withVedtakStartet(nyesteVedtak)
                .withStatus(BeslutteroversiktStatus.TRENGER_BESLUTTER));
        beslutteroversiktRepository.lagBruker(lagBruker(opprettVedtakForTest("lik-dato-2"))
                .withBrukerOppfolgingsenhetId("9902")
                .withBrukerOppfolgingsenhetNavn("Lik dato, høyere rad-ID")
                .withVedtakStartet(nyesteVedtak)
                .withStatus(BeslutteroversiktStatus.KLAR_TIL_BESLUTTER));
        beslutteroversiktRepository.lagBruker(lagBruker(opprettVedtakForTest("godkjent"))
                .withBrukerOppfolgingsenhetId("9902")
                .withBrukerOppfolgingsenhetNavn("Nyeste ikke-aktivt utkast")
                .withVedtakStartet(nyesteIkkeAktiveVedtak)
                .withStatus(BeslutteroversiktStatus.GODKJENT_AV_BESLUTTER));
        beslutteroversiktRepository.lagBruker(lagBruker(opprettVedtakForTest("annen-enhet"))
                .withBrukerOppfolgingsenhetId("9901")
                .withBrukerOppfolgingsenhetNavn("Enhet først")
                .withStatus(BeslutteroversiktStatus.KLAR_TIL_VEILEDER));

        assertEquals(of(
                        new EnhetMedNavn("9901", "Enhet først"),
                        new EnhetMedNavn("9902", "Lik dato, høyere rad-ID")),
                beslutteroversiktRepository.hentEnheterMedUtkast());
    }

    private long opprettVedtakForTest(String aktorId) {
        vedtaksstotteRepository.opprettUtkast(aktorId, TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID);
        return vedtaksstotteRepository.hentUtkast(aktorId).getId();
    }

    private BeslutteroversiktBruker lagBruker(long vedtakId) {
        return new BeslutteroversiktBruker(
                vedtakId,
                "fornavn",
                "etternavn",
                TEST_OPPFOLGINGSENHET_NAVN,
                TEST_OPPFOLGINGSENHET_ID,
                TEST_FNR.get(),
                now(),
                BeslutteroversiktStatus.KLAR_TIL_BESLUTTER,
                null, // statusEndret settes av databasen
                TEST_BESLUTTER_NAVN,
                TEST_BESLUTTER_IDENT,
                TEST_VEILEDER_NAVN
        );
    }

}
