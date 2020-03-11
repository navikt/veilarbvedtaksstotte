package no.nav.veilarbvedtaksstotte.service;

import no.nav.veilarbvedtaksstotte.client.SAFClient;
import no.nav.veilarbvedtaksstotte.domain.ArkivertVedtak;
import no.nav.veilarbvedtaksstotte.domain.Journalpost;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ArenaVedtakService {

    final static String JOURNALPOST_ARENA_VEDTAK_TITTEL = "Brev: Oppfølgingsvedtak (§14a)";

    private SAFClient safClient;
    private AuthService authService;

    @Inject
    public ArenaVedtakService(SAFClient safClient, AuthService authService) {
        this.safClient = safClient;
        this.authService = authService;
    }

    public List<ArkivertVedtak> hentVedtakFraArena(String fnr) {
        authService.sjekkTilgang(fnr);
        return hentArkiverteVedtakFraArena(fnr);
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
