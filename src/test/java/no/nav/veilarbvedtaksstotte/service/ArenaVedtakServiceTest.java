package no.nav.veilarbvedtaksstotte.service;

import no.nav.common.types.identer.Fnr;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.Journalpost;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.SafClient;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.VeilarbveilederClient;
import no.nav.veilarbvedtaksstotte.domain.arkiv.ArkivertVedtak;
import no.nav.veilarbvedtaksstotte.domain.vedtak.ArenaVedtak;
import no.nav.veilarbvedtaksstotte.repository.ArenaVedtakRepository;
import no.nav.veilarbvedtaksstotte.utils.DatabaseTest;
import no.nav.veilarbvedtaksstotte.utils.TestData;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static no.nav.veilarbvedtaksstotte.service.ArenaVedtakService.MODIA_REG_USER;
import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_FNR;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ArenaVedtakServiceTest extends DatabaseTest {

    private static ArenaVedtakService service;
    private static ArenaVedtakRepository arenaVedtakRepository;
    private static final VeilarbveilederClient veiledereOgEnhetClient = mock(VeilarbveilederClient.class);
    private static final SafClient safClient = mock(SafClient.class);

    @BeforeClass
    public static void setup() {
        arenaVedtakRepository = new ArenaVedtakRepository(jdbcTemplate);
        service = new ArenaVedtakService(arenaVedtakRepository, safClient, null);
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
        assertTrue(service.behandleVedtakFraArena(
                new ArenaVedtak(
                        fnr,
                        ArenaVedtak.ArenaInnsatsgruppe.BATT,
                        ArenaVedtak.ArenaHovedmal.BEHOLDEA,
                        LocalDate.now(),
                        "reg user",
                        LocalDateTime.now(),
                        12345,
                        1
                )
        ));

        ArenaVedtak arenaVedtak = arenaVedtakRepository.hentVedtak(fnr);
        assertNotNull(arenaVedtak);
    }

    @Test
    public void behandleVedtakFraArena__overskriver_lagret_vedtak_med_vedtak_som_har_nyere_fra_dato() {
        Fnr fnr = Fnr.of(randomNumeric(10));
        ArenaVedtak arenaVedtak1 = new ArenaVedtak(
                fnr,
                ArenaVedtak.ArenaInnsatsgruppe.BATT,
                ArenaVedtak.ArenaHovedmal.BEHOLDEA,
                LocalDate.now(),
                "reg user",
                LocalDateTime.now().plusDays(3),
                1234,
                1
        );

        ArenaVedtak arenaVedtak2 = new ArenaVedtak(
                fnr,
                ArenaVedtak.ArenaInnsatsgruppe.BFORM,
                ArenaVedtak.ArenaHovedmal.SKAFFEA,
                arenaVedtak1.getFraDato().plusDays(1),
                "reg user",
                arenaVedtak1.getOperationTimestamp(),
                4321,
                1

        );

        assertTrue(service.behandleVedtakFraArena(arenaVedtak1));
        assertTrue(service.behandleVedtakFraArena(arenaVedtak2));

        ArenaVedtak lagretArenaVedtak = arenaVedtakRepository.hentVedtak(fnr);


        assertEquals(arenaVedtak2, lagretArenaVedtak);
        assertNotEquals(arenaVedtak1, lagretArenaVedtak);
    }

    @Test
    public void behandleVedtakFraArena__overskriver_lagret_vedtak_med_vedtak_som_har_nyere_operation_timestamp() {
        Fnr fnr = Fnr.of(randomNumeric(10));
        ArenaVedtak arenaVedtak1 = new ArenaVedtak(
                fnr,
                ArenaVedtak.ArenaInnsatsgruppe.BATT,
                ArenaVedtak.ArenaHovedmal.BEHOLDEA,
                LocalDate.now(),
                "reg user",
                LocalDateTime.now(),
                1234,
                1
        );

        ArenaVedtak arenaVedtak2 = new ArenaVedtak(
                fnr,
                ArenaVedtak.ArenaInnsatsgruppe.BFORM,
                ArenaVedtak.ArenaHovedmal.SKAFFEA,
                arenaVedtak1.getFraDato(),
                "reg user",
                arenaVedtak1.getOperationTimestamp().plusMinutes(1),
                4321,
                1

        );

        assertTrue(service.behandleVedtakFraArena(arenaVedtak1));
        assertTrue(service.behandleVedtakFraArena(arenaVedtak2));

        ArenaVedtak lagretArenaVedtak = arenaVedtakRepository.hentVedtak(fnr);


        assertEquals(arenaVedtak2, lagretArenaVedtak);
        assertNotEquals(arenaVedtak1, lagretArenaVedtak);
    }

    @Test
    public void behandleVedtakFraArena__lagrer_ikke_vedtak_som_stammer_fra_denne_losningen() {
        Fnr fnr = Fnr.of(randomNumeric(10));
        assertFalse(service.behandleVedtakFraArena(
                new ArenaVedtak(fnr,
                        ArenaVedtak.ArenaInnsatsgruppe.BATT,
                        ArenaVedtak.ArenaHovedmal.BEHOLDEA,
                        LocalDate.now(),
                        MODIA_REG_USER,
                        LocalDateTime.now(),
                        12345,
                        1
                )
        ));

        ArenaVedtak arenaVedtak = arenaVedtakRepository.hentVedtak(fnr);
        assertNull(arenaVedtak);
    }

    @Test
    public void behandleVedtakFraArena__overskriver_ikke_lagret_vedtak_med_vedtak_som_har_lik_dato_og_lik_hendelses_id() {
        Fnr fnr = Fnr.of(randomNumeric(10));
        ArenaVedtak arenaVedtak1 = new ArenaVedtak(
                fnr,
                ArenaVedtak.ArenaInnsatsgruppe.BATT,
                ArenaVedtak.ArenaHovedmal.BEHOLDEA,
                LocalDate.now().minusDays(1),
                "reg user",
                LocalDateTime.now(),
                1234,
                1
        );

        ArenaVedtak arenaVedtak2 = new ArenaVedtak(
                fnr,
                arenaVedtak1.getInnsatsgruppe(),
                arenaVedtak1.getHovedmal(),
                arenaVedtak1.getFraDato(),
                arenaVedtak1.getRegUser(),
                arenaVedtak1.getOperationTimestamp(),
                1234,
                1
        );

        assertTrue(service.behandleVedtakFraArena(arenaVedtak1));
        assertFalse(service.behandleVedtakFraArena(arenaVedtak2));

        ArenaVedtak lagretArenaVedtak = arenaVedtakRepository.hentVedtak(fnr);


        assertEquals(arenaVedtak2, lagretArenaVedtak);
        assertEquals(arenaVedtak1, lagretArenaVedtak);
    }

    @Test
    public void behandleVedtakFraArena__overskriver_lagret_vedtak_med_vedtak_som_har_lik_dato_og_ulik_hendelses_id() {
        Fnr fnr = Fnr.of(randomNumeric(10));
        ArenaVedtak arenaVedtak1 = new ArenaVedtak(
                fnr,
                ArenaVedtak.ArenaInnsatsgruppe.BATT,
                ArenaVedtak.ArenaHovedmal.BEHOLDEA,
                LocalDate.now().minusDays(1),
                "reg user",
                LocalDateTime.now(),
                1234,
                1
        );

        ArenaVedtak arenaVedtak2 = new ArenaVedtak(
                fnr,
                ArenaVedtak.ArenaInnsatsgruppe.BFORM,
                ArenaVedtak.ArenaHovedmal.SKAFFEA,
                arenaVedtak1.getFraDato(),
                "reg user",
                arenaVedtak1.getOperationTimestamp(),
                4321,
                1
        );

        assertTrue(service.behandleVedtakFraArena(arenaVedtak1));
        assertTrue(service.behandleVedtakFraArena(arenaVedtak2));

        ArenaVedtak lagretArenaVedtak = arenaVedtakRepository.hentVedtak(fnr);


        assertNotEquals(arenaVedtak1, lagretArenaVedtak);
        assertEquals(arenaVedtak2, lagretArenaVedtak);
    }

    @Test
    public void behandleVedtakFraArena__overskriver_ikke_lagret_vedtak_med_vedtak_som_har_lik_hendelse_id_og_ulikt_innhold() {
        Fnr fnr = Fnr.of(randomNumeric(10));
        ArenaVedtak arenaVedtak1 = new ArenaVedtak(
                fnr,
                ArenaVedtak.ArenaInnsatsgruppe.BATT,
                ArenaVedtak.ArenaHovedmal.BEHOLDEA,
                LocalDate.now().minusDays(1),
                "reg user",
                LocalDateTime.now(),
                12345,
                1
        );

        ArenaVedtak arenaVedtak2 = new ArenaVedtak(
                fnr,
                ArenaVedtak.ArenaInnsatsgruppe.BFORM,
                ArenaVedtak.ArenaHovedmal.SKAFFEA,
                arenaVedtak1.getFraDato(),
                "reg user",
                LocalDateTime.now().plusDays(1),
                arenaVedtak1.getHendelseId(),
                arenaVedtak1.getVedtakId()
        );

        assertTrue(service.behandleVedtakFraArena(arenaVedtak1));
        assertFalse(service.behandleVedtakFraArena(arenaVedtak2));

        ArenaVedtak lagretArenaVedtak = arenaVedtakRepository.hentVedtak(fnr);


        assertEquals(arenaVedtak1, lagretArenaVedtak);
        assertNotEquals(arenaVedtak2, lagretArenaVedtak);
    }

    @Test
    public void behandleVedtakFraArena__overskriver_ikke_lagret_vedtak_med_vedtak_som_har_fra_eldre_dato() {
        Fnr fnr = Fnr.of(randomNumeric(10));
        ArenaVedtak arenaVedtak1 = new ArenaVedtak(
                fnr,
                ArenaVedtak.ArenaInnsatsgruppe.BATT,
                ArenaVedtak.ArenaHovedmal.BEHOLDEA,
                LocalDate.now(),
                "reg user",
                LocalDateTime.now(),
                12345,
                1
        );

        ArenaVedtak arenaVedtak2 = new ArenaVedtak(
                fnr,
                ArenaVedtak.ArenaInnsatsgruppe.BFORM,
                ArenaVedtak.ArenaHovedmal.SKAFFEA,
                arenaVedtak1.getFraDato().minusDays(1),
                "reg user",
                arenaVedtak1.getOperationTimestamp(),
                12346,
                1
        );

        assertTrue(service.behandleVedtakFraArena(arenaVedtak1));
        assertFalse(service.behandleVedtakFraArena(arenaVedtak2));

        ArenaVedtak lagretArenaVedtak = arenaVedtakRepository.hentVedtak(fnr);


        assertNotEquals(arenaVedtak2, lagretArenaVedtak);
        assertEquals(arenaVedtak1, lagretArenaVedtak);
    }

    @Test
    public void behandleVedtakFraArena__overskriver_ikke_lagret_vedtak_med_vedtak_som_har_eldre_operation_timestamp() {
        Fnr fnr = Fnr.of(randomNumeric(10));
        ArenaVedtak arenaVedtak1 = new ArenaVedtak(
                fnr,
                ArenaVedtak.ArenaInnsatsgruppe.BATT,
                ArenaVedtak.ArenaHovedmal.BEHOLDEA,
                LocalDate.now(),
                "reg user",
                LocalDateTime.now(),
                12345,
                1
        );

        ArenaVedtak arenaVedtak2 = new ArenaVedtak(
                fnr,
                ArenaVedtak.ArenaInnsatsgruppe.BFORM,
                ArenaVedtak.ArenaHovedmal.SKAFFEA,
                arenaVedtak1.getFraDato(),
                "reg user",
                arenaVedtak1.getOperationTimestamp().minusMinutes(1),
                12346,
                1
        );

        assertTrue(service.behandleVedtakFraArena(arenaVedtak1));
        assertFalse(service.behandleVedtakFraArena(arenaVedtak2));

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
