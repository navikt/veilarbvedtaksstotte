package no.nav.veilarbvedtaksstotte.service;

import no.nav.veilarbvedtaksstotte.client.dokarkiv.SafClient;
import no.nav.veilarbvedtaksstotte.domain.arkiv.ArkivertVedtak;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.Journalpost;
import no.nav.veilarbvedtaksstotte.domain.vedtak.ArenaVedtak;
import no.nav.veilarbvedtaksstotte.repository.ArenaVedtakRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ArenaVedtakService {

    final static String JOURNALPOST_ARENA_VEDTAK_TITTEL = "Brev: Oppfølgingsvedtak (§14a)";
    final static String VEILARBVEDAKSSTOTTE_MOD_USER = "VEILARBVEDAKSSTOTTE_MOD_USER"; // TODO riktig verdi

    private ArenaVedtakRepository arenaVedtakRepository;
    private SafClient safClient;
    private AuthService authService;

    @Autowired
    public ArenaVedtakService(ArenaVedtakRepository arenaVedtakRepository,
                              SafClient safClient,
                              AuthService authService) {
        this.arenaVedtakRepository = arenaVedtakRepository;
        this.safClient = safClient;
        this.authService = authService;
    }

    public List<ArkivertVedtak> hentVedtakFraArena(String fnr) {
        authService.sjekkTilgangTilFnr(fnr);
        return hentArkiverteVedtakFraArena(fnr);
    }

    public byte[] hentVedtakPdf(String dokumentInfoId, String journalpostId) {
        // Tilgangskontroll gjøres av SAF
        return safClient.hentVedtakPdf(journalpostId, dokumentInfoId);
    }

    public void behandleVedtakFraArena(ArenaVedtak arenaVedtak) {

        if (VEILARBVEDAKSSTOTTE_MOD_USER.equals(arenaVedtak.getModUser())) {
            return;
        }

        ArenaVedtak eksisterendeVedtak = arenaVedtakRepository.hentVedtak(arenaVedtak.getFnr());

        if (eksisterendeVedtak != null && !arenaVedtak.getFraDato().isAfter(eksisterendeVedtak.getFraDato())) {
            return;
        }

        arenaVedtakRepository.upsertVedtak(arenaVedtak);
    }

    protected List<ArkivertVedtak> hentArkiverteVedtakFraArena(String fnr) {
        return safClient.hentJournalposter(fnr)
                .stream()
                .filter(this::erVedtakFraArena)
                .map(this::tilArkivertVedtak)
                .filter(this::harDokumentInfoId)
                .collect(Collectors.toList());
    }

    private ArkivertVedtak tilArkivertVedtak(Journalpost journalpost) {
        ArkivertVedtak arkivertVedtak = new ArkivertVedtak();

        arkivertVedtak.journalpostId = journalpost.journalpostId;

        if (journalpost.dokumenter != null && journalpost.dokumenter.length > 0) {
            Journalpost.JournalpostDokument dokument = journalpost.dokumenter[0];
            arkivertVedtak.dokumentInfoId = dokument.dokumentInfoId;
            arkivertVedtak.dato = Optional.ofNullable(dokument.datoFerdigstilt)
                    .map(LocalDateTime::parse)
                    .orElse(null);
        }

        return arkivertVedtak;
    }

    private boolean erVedtakFraArena(Journalpost journalpost) {
        return JOURNALPOST_ARENA_VEDTAK_TITTEL.equals(journalpost.tittel);
    }

    private boolean harDokumentInfoId(ArkivertVedtak arkivertVedtak) {
        return arkivertVedtak.dokumentInfoId != null;
    }

}
