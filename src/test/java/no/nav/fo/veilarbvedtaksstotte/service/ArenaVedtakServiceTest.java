package no.nav.fo.veilarbvedtaksstotte.service;

import no.nav.fo.veilarbvedtaksstotte.client.SAFClient;
import no.nav.fo.veilarbvedtaksstotte.client.VeiledereOgEnhetClient;
import no.nav.fo.veilarbvedtaksstotte.domain.ArkivertVedtak;
import no.nav.fo.veilarbvedtaksstotte.domain.Journalpost;
import no.nav.fo.veilarbvedtaksstotte.utils.TestData;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static no.nav.fo.veilarbvedtaksstotte.service.ArenaVedtakService.JOURNALPOST_ARENA_VEDTAK_TITTEL;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ArenaVedtakServiceTest {

    private static VeiledereOgEnhetClient veiledereOgEnhetClient = mock(VeiledereOgEnhetClient.class);

    static {
        when(veiledereOgEnhetClient.hentEnhetNavn(any())).thenReturn("TEST");
    }

    @Test
    public void hentArkiverteVedtakFraArena__skalFiltrereVekkVedtakMedFeilTittel() {
        SAFClient safClient = mock(SAFClient.class);
        ArenaVedtakService service = new ArenaVedtakService(safClient, null);

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
        ArenaVedtakService arenaVedtakService = new ArenaVedtakService( safClient,null);

        List<Journalpost> journalposter = new ArrayList<>();
        Journalpost journalpostUtenDokumentId = lagJournalpost(JOURNALPOST_ARENA_VEDTAK_TITTEL);
        journalpostUtenDokumentId.dokumenter = null;

        journalposter.add(lagJournalpost(JOURNALPOST_ARENA_VEDTAK_TITTEL));
        journalposter.add(journalpostUtenDokumentId);

        when(safClient.hentJournalposter(TestData.TEST_FNR)).thenReturn(journalposter);

        List<ArkivertVedtak> vedtakFraArena = arenaVedtakService.hentArkiverteVedtakFraArena(TestData.TEST_FNR);

        assertEquals(1, vedtakFraArena.size());
    }

    private Journalpost lagJournalpost(String tittel) {
        Journalpost journalpost = new Journalpost();
        journalpost.tittel = tittel;

        Journalpost.JournalpostDokument dokument = new Journalpost.JournalpostDokument();
        dokument.dokumentInfoId = TestData.TEST_DOKUMENT_ID;
        dokument.datoFerdigstilt = LocalDateTime.now().toString();
        journalpost.dokumenter = new Journalpost.JournalpostDokument[]{dokument};

        return journalpost;
    }

}
