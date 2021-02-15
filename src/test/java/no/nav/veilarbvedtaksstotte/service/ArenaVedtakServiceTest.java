package no.nav.veilarbvedtaksstotte.service;

import no.nav.common.types.identer.Fnr;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.SafClient;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.VeilarbveilederClient;
import no.nav.veilarbvedtaksstotte.domain.arkiv.ArkivertVedtak;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.Journalpost;
import no.nav.veilarbvedtaksstotte.domain.vedtak.ArenaVedtak;
import no.nav.veilarbvedtaksstotte.repository.ArenaVedtakRepository;
import no.nav.veilarbvedtaksstotte.utils.SingletonPostgresContainer;
import no.nav.veilarbvedtaksstotte.utils.TestData;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static no.nav.veilarbvedtaksstotte.service.ArenaVedtakService.VEILARBVEDAKSSTOTTE_REG_USER;
import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_FNR;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ArenaVedtakServiceTest {

    private static ArenaVedtakService service;
    private static JdbcTemplate jdbcTemplate;
    private static ArenaVedtakRepository arenaVedtakRepository;
    private static VeilarbveilederClient veiledereOgEnhetClient = mock(VeilarbveilederClient.class);
    private static SafClient safClient = mock(SafClient.class);

    @BeforeClass
    public static void setup() {
        jdbcTemplate = SingletonPostgresContainer.init().getDb();
        arenaVedtakRepository = new ArenaVedtakRepository(jdbcTemplate);
        service = new ArenaVedtakService(arenaVedtakRepository, safClient, null, null);
        when(veiledereOgEnhetClient.hentEnhetNavn(any())).thenReturn("TEST");
    }

    @Test
    public void hentArkiverteVedtakFraArena__skalFiltrereVekkVedtakMedFeilTittel() {
        List<Journalpost> journalposter = new ArrayList<>();
        journalposter.add(lagJournalpost(ArenaVedtakService.JOURNALPOST_ARENA_VEDTAK_TITTEL));
        journalposter.add(lagJournalpost("En tittel"));

        when(safClient.hentJournalposter(TEST_FNR)).thenReturn(journalposter);

        List<ArkivertVedtak> vedtakFraArena = service.hentArkiverteVedtakFraArena(TEST_FNR);

        assertEquals(1, vedtakFraArena.size());
    }

    @Test
    public void hentArkiverteVedtakFraArena__skalFiltrereVekkVedtakUtenDokumentId() {
        List<Journalpost> journalposter = new ArrayList<>();
        Journalpost journalpostUtenDokumentId = lagJournalpost(ArenaVedtakService.JOURNALPOST_ARENA_VEDTAK_TITTEL);
        journalpostUtenDokumentId.dokumenter = null;

        journalposter.add(lagJournalpost(ArenaVedtakService.JOURNALPOST_ARENA_VEDTAK_TITTEL));
        journalposter.add(journalpostUtenDokumentId);

        when(safClient.hentJournalposter(TEST_FNR)).thenReturn(journalposter);

        List<ArkivertVedtak> vedtakFraArena = service.hentArkiverteVedtakFraArena(TEST_FNR);

        assertEquals(1, vedtakFraArena.size());
    }

    @Test
    public void behandleVedtakFraArena__lagrer_vedtak_fra_arena() {
        Fnr fnr = Fnr.of(randomNumeric(10));
        service.behandleVedtakFraArena(
                new ArenaVedtak(
                        fnr,
                        ArenaVedtak.ArenaInnsatsgruppe.BATT,
                        ArenaVedtak.ArenaHovedmal.BEHOLDE_ARBEID,
                        LocalDateTime.now(),
                        "MOD USER"
                )
        );

        ArenaVedtak arenaVedtak = arenaVedtakRepository.hentVedtak(fnr);
        assertNotNull(arenaVedtak);
    }

    @Test
    public void behandleVedtakFraArena__lagrer_ikke_vedtak_som_stammer_fra_denne_losningen() {
        Fnr fnr = Fnr.of(randomNumeric(10));
        service.behandleVedtakFraArena(
                new ArenaVedtak(fnr,
                        ArenaVedtak.ArenaInnsatsgruppe.BATT,
                        ArenaVedtak.ArenaHovedmal.BEHOLDE_ARBEID,
                        LocalDateTime.now(),
                        VEILARBVEDAKSSTOTTE_REG_USER
                )
        );

        ArenaVedtak arenaVedtak = arenaVedtakRepository.hentVedtak(fnr);
        assertNull(arenaVedtak);
    }

    @Test
    public void behandleVedtakFraArena__overskriver_ikke_lagret_vedtak_med_vedtak_som_har_lik_dato() {
        Fnr fnr = Fnr.of(randomNumeric(10));
        ArenaVedtak arenaVedtak1 = new ArenaVedtak(
                fnr,
                ArenaVedtak.ArenaInnsatsgruppe.BATT,
                ArenaVedtak.ArenaHovedmal.BEHOLDE_ARBEID,
                LocalDateTime.now(),
                "mod user"
        );

        ArenaVedtak arenaVedtak2 = new ArenaVedtak(
                fnr,
                ArenaVedtak.ArenaInnsatsgruppe.BFORM,
                ArenaVedtak.ArenaHovedmal.SKAFFE_ARBEID,
                arenaVedtak1.getFraDato(),
                "mod user"
        );

        service.behandleVedtakFraArena(arenaVedtak1);
        service.behandleVedtakFraArena(arenaVedtak2);

        ArenaVedtak lagretArenaVedtak = arenaVedtakRepository.hentVedtak(fnr);


        assertNotEquals(arenaVedtak2, lagretArenaVedtak);
        assertEquals(arenaVedtak1, lagretArenaVedtak);
    }

    @Test
    public void behandleVedtakFraArena__overskriver_ikke_lagret_vedtak_med_vedtak_som_har_eldre_dato() {
        Fnr fnr = Fnr.of(randomNumeric(10));
        ArenaVedtak arenaVedtak1 = new ArenaVedtak(
                fnr,
                ArenaVedtak.ArenaInnsatsgruppe.BATT,
                ArenaVedtak.ArenaHovedmal.BEHOLDE_ARBEID,
                LocalDateTime.now(),
                "mod user"
        );

        ArenaVedtak arenaVedtak2 = new ArenaVedtak(
                fnr,
                ArenaVedtak.ArenaInnsatsgruppe.BFORM,
                ArenaVedtak.ArenaHovedmal.SKAFFE_ARBEID,
                arenaVedtak1.getFraDato().minusMinutes(1),
                "mod user"
        );

        service.behandleVedtakFraArena(arenaVedtak1);
        service.behandleVedtakFraArena(arenaVedtak2);

        ArenaVedtak lagretArenaVedtak = arenaVedtakRepository.hentVedtak(fnr);


        assertNotEquals(arenaVedtak2, lagretArenaVedtak);
        assertEquals(arenaVedtak1, lagretArenaVedtak);
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
