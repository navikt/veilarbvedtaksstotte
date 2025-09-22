package no.nav.veilarbvedtaksstotte.repository;

import no.nav.veilarbvedtaksstotte.domain.beslutteroversikt.BeslutteroversiktBruker;
import no.nav.veilarbvedtaksstotte.domain.beslutteroversikt.BeslutteroversiktStatus;
import no.nav.veilarbvedtaksstotte.utils.DatabaseTest;
import no.nav.veilarbvedtaksstotte.utils.DbTestUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
                .setStatusEndret(null);

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
                .setStatus(BeslutteroversiktStatus.TRENGER_BESLUTTER);

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

    private BeslutteroversiktBruker lagBruker(long vedtakId) {
        return new BeslutteroversiktBruker()
                .setVedtakId(vedtakId)
                .setBrukerFornavn("fornavn")
                .setBrukerEtternavn("etternavn")
                .setBrukerOppfolgingsenhetNavn(TEST_OPPFOLGINGSENHET_NAVN)
                .setBrukerOppfolgingsenhetId(TEST_OPPFOLGINGSENHET_ID)
                .setBrukerFnr(TEST_FNR.get())
                .setVedtakStartet(now())
                .setStatus(BeslutteroversiktStatus.KLAR_TIL_BESLUTTER)
                .setBeslutterNavn(TEST_BESLUTTER_NAVN)
                .setBeslutterIdent(TEST_BESLUTTER_IDENT)
                .setVeilederNavn(TEST_VEILEDER_NAVN);
    }

}
