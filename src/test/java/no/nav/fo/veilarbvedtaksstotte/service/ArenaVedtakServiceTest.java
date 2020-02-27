package no.nav.fo.veilarbvedtaksstotte.service;

import no.nav.fo.veilarbvedtaksstotte.client.OppfolgingClient;
import no.nav.fo.veilarbvedtaksstotte.client.SAFClient;
import no.nav.fo.veilarbvedtaksstotte.client.VeiledereOgEnhetClient;
import no.nav.fo.veilarbvedtaksstotte.domain.*;
import no.nav.fo.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import no.nav.fo.veilarbvedtaksstotte.utils.TestData;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static no.nav.fo.veilarbvedtaksstotte.service.ArenaVedtakService.JOURNALPOST_ARENA_VEDTAK_TITTEL;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ArenaVedtakServiceTest {

    private static VeiledereOgEnhetClient veiledereOgEnhetClient = mock(VeiledereOgEnhetClient.class);

    static {
        when(veiledereOgEnhetClient.hentEnhetNavn(any())).thenReturn("TEST");
    }

    @Test
    public void hentVedtakFraArena__skalReturnereListeHvorGjeldendeVedtakMedInnsatsgruppeFinnes() {
        VedtaksstotteRepository vedtaksstotteRepository = mock(VedtaksstotteRepository.class);
        OppfolgingClient oppfolgingClient = mock(OppfolgingClient.class);
        AuthService authService = mock(AuthService.class);
        SAFClient safClient = mock(SAFClient.class);
        ArenaVedtakService service = new ArenaVedtakService(
                vedtaksstotteRepository, safClient, veiledereOgEnhetClient, authService, oppfolgingClient
        );

        AuthKontekst authKontekst = new AuthKontekst();
        authKontekst.setAktorId(TestData.TEST_AKTOR_ID);
        authKontekst.setFnr(TestData.TEST_FNR);

        OppfolgingDTO oppfolgingData = new OppfolgingDTO();
        oppfolgingData.setServicegruppe("IKVAL");

        List<OppfolgingPeriodeDTO> oppfolgingsperioder = new ArrayList<>();
        OppfolgingPeriodeDTO oppfolgingPeriode = new OppfolgingPeriodeDTO();
        oppfolgingPeriode.startDato = LocalDateTime.now().minusMinutes(30);
        oppfolgingPeriode.sluttDato = null;
        oppfolgingsperioder.add(oppfolgingPeriode);
        oppfolgingData.setOppfolgingsPerioder(oppfolgingsperioder);

        List<Journalpost> journalposter = new ArrayList<>();
        journalposter.add(lagJournalpost(JOURNALPOST_ARENA_VEDTAK_TITTEL));

        when(authService.sjekkTilgang(TestData.TEST_FNR)).thenReturn(authKontekst);
        when(safClient.hentJournalposter(TestData.TEST_FNR)).thenReturn(journalposter);
        when(vedtaksstotteRepository.harGjeldendeVedtak(TestData.TEST_AKTOR_ID)).thenReturn(false);
        when(oppfolgingClient.hentOppfolgingData(TestData.TEST_FNR)).thenReturn(oppfolgingData);

        List<ArkivertVedtak> vedtakFraArena = service.hentVedtakFraArena(TestData.TEST_FNR);

        assertFalse(vedtakFraArena.isEmpty());
        assertTrue(vedtakFraArena.get(0).erGjeldende);
        assertNotNull(vedtakFraArena.get(0).innsatsgruppe);
    }

    @Test
    public void finnGjeldendeVedtakFraArena__skalReturnereGjeldendeVedtak() {
        VedtaksstotteRepository vedtaksstotteRepository = mock(VedtaksstotteRepository.class);
        OppfolgingClient oppfolgingClient = mock(OppfolgingClient.class);
        ArenaVedtakService service = new ArenaVedtakService(vedtaksstotteRepository, null, veiledereOgEnhetClient, null, oppfolgingClient);

        OppfolgingDTO oppfolgingData = new OppfolgingDTO();
        List<OppfolgingPeriodeDTO> oppfolgingsperioder = new ArrayList<>();

        OppfolgingPeriodeDTO oppfolgingPeriode = new OppfolgingPeriodeDTO();
        oppfolgingPeriode.startDato = LocalDateTime.now().minusMinutes(30);
        oppfolgingPeriode.sluttDato = null;
        oppfolgingsperioder.add(oppfolgingPeriode);

        oppfolgingData.setOppfolgingsPerioder(oppfolgingsperioder);

        when(vedtaksstotteRepository.harGjeldendeVedtak(TestData.TEST_AKTOR_ID)).thenReturn(false);
        when(oppfolgingClient.hentOppfolgingData(TestData.TEST_FNR)).thenReturn(oppfolgingData);

        List<ArkivertVedtak> arkiverteVedtak = new ArrayList<>();
        ArkivertVedtak arkivertVedtak = new ArkivertVedtak();
        arkivertVedtak.datoOpprettet = LocalDateTime.now();
        arkiverteVedtak.add(arkivertVedtak);

        Optional<ArkivertVedtak> gjeldendeVedtak = service.finnGjeldendeVedtakFraArena(arkiverteVedtak, TestData.TEST_FNR, TestData.TEST_AKTOR_ID);

        assertTrue(gjeldendeVedtak.isPresent());
    }

    @Test
    public void finnGjeldendeVedtakFraArena__skalReturnereEmptyHvisVedtaksstotteHarGjeldendeVedtak() {
        VedtaksstotteRepository vedtaksstotteRepository = mock(VedtaksstotteRepository.class);
        ArenaVedtakService service = new ArenaVedtakService(vedtaksstotteRepository, null, veiledereOgEnhetClient, null, null);

        when(vedtaksstotteRepository.harGjeldendeVedtak(TestData.TEST_AKTOR_ID)).thenReturn(true);

        List<ArkivertVedtak> arkiverteVedtak = new ArrayList<>();
        ArkivertVedtak vedtak1 = new ArkivertVedtak();
        vedtak1.datoOpprettet = LocalDateTime.now().minusMinutes(30);

        ArkivertVedtak vedtak2 = new ArkivertVedtak();
        vedtak2.datoOpprettet = LocalDateTime.now().minusMinutes(100);

        arkiverteVedtak.add(vedtak1);
        arkiverteVedtak.add(vedtak2);

        assertFalse(service.finnGjeldendeVedtakFraArena(arkiverteVedtak, TestData.TEST_FNR, TestData.TEST_AKTOR_ID).isPresent());
    }

    @Test
    public void finnGjeldendeVedtakFraArena__skalReturnereEmptyHvisIngenVedtakFraArenaFinnes() {
        VedtaksstotteRepository vedtaksstotteRepository = mock(VedtaksstotteRepository.class);
        ArenaVedtakService service = new ArenaVedtakService(vedtaksstotteRepository, null, veiledereOgEnhetClient, null, null);

        when(vedtaksstotteRepository.harGjeldendeVedtak(TestData.TEST_AKTOR_ID)).thenReturn(false);

        assertFalse(service.finnGjeldendeVedtakFraArena(new ArrayList<>(), TestData.TEST_FNR, TestData.TEST_AKTOR_ID).isPresent());
    }

    @Test
    public void hentArkiverteVedtakFraArena__skalFiltrereVekkVedtakMedFeilTittel() {
        SAFClient safClient = mock(SAFClient.class);
        ArenaVedtakService service = new ArenaVedtakService(null, safClient, veiledereOgEnhetClient, null, null);

        List<Journalpost> journalposter = new ArrayList<>();
        journalposter.add(lagJournalpost(JOURNALPOST_ARENA_VEDTAK_TITTEL));
        journalposter.add(lagJournalpost("En tittel"));

        when(safClient.hentJournalposter(TestData.TEST_FNR)).thenReturn(journalposter);

        List<ArkivertVedtak> vedtakFraArena = service.hentArkiverteVedtakFraArena(TestData.TEST_FNR);

        assertEquals(1, vedtakFraArena.size());
    }

    @Test
    public void hentArkiverteVedtakFraArena__skalFiltrereVekkVedtakUtenDokumentId() {
        SAFClient safClient = mock(SAFClient.class);
        ArenaVedtakService arenaVedtakService = new ArenaVedtakService(null, safClient, veiledereOgEnhetClient, null, null);

        List<Journalpost> journalposter = new ArrayList<>();
        Journalpost journalpostUtenDokumentId = lagJournalpost(JOURNALPOST_ARENA_VEDTAK_TITTEL);
        journalpostUtenDokumentId.dokumenter = null;

        journalposter.add(lagJournalpost(JOURNALPOST_ARENA_VEDTAK_TITTEL));
        journalposter.add(journalpostUtenDokumentId);

        when(safClient.hentJournalposter(TestData.TEST_FNR)).thenReturn(journalposter);

        List<ArkivertVedtak> vedtakFraArena = arenaVedtakService.hentArkiverteVedtakFraArena(TestData.TEST_FNR);

        assertEquals(1, vedtakFraArena.size());
    }

    @Test
    public void finnSisteArkiverteVedtak__skalFinneSisteVedtak() {
        List<ArkivertVedtak> arkivertVedtak = new ArrayList<>();

        ArkivertVedtak vedtak1 = new ArkivertVedtak();
        vedtak1.datoOpprettet = LocalDateTime.now().minusMinutes(30);

        ArkivertVedtak vedtak2 = new ArkivertVedtak();
        vedtak2.datoOpprettet = LocalDateTime.now().minusMinutes(100);

        arkivertVedtak.add(vedtak1);
        arkivertVedtak.add(vedtak2);

        Optional<ArkivertVedtak> kanskjeVedtak = ArenaVedtakService.finnSisteArkiverteVedtak(arkivertVedtak);

        assertTrue(kanskjeVedtak.isPresent());
        assertEquals(kanskjeVedtak.get().datoOpprettet, vedtak1.datoOpprettet);
    }

    private Journalpost lagJournalpost(String tittel) {
        Journalpost journalpost = new Journalpost();
        journalpost.tittel = tittel;
        journalpost.datoOpprettet = LocalDateTime.now().toString();

        Journalpost.JournalpostDokument dokument = new Journalpost.JournalpostDokument();
        dokument.dokumentInfoId = TestData.TEST_DOKUMENT_ID;
        journalpost.dokumenter = new Journalpost.JournalpostDokument[]{dokument};

        return journalpost;
    }

}
