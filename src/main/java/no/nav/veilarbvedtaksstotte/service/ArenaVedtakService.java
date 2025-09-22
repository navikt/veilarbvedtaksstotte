package no.nav.veilarbvedtaksstotte.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.Fnr;
import no.nav.poao_tilgang.client.TilgangType;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.SafClient;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.dto.Journalpost;
import no.nav.veilarbvedtaksstotte.domain.arkiv.ArkivertVedtak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ArenaVedtakService {

    final static String JOURNALPOST_ARENA_VEDTAK_TITTEL = "Brev: Oppfølgingsvedtak (§14a)";

    private final SafClient safClient;
    private final AuthService authService;

    @Autowired
    public ArenaVedtakService(
            SafClient safClient,
            AuthService authService
    ) {
        this.safClient = safClient;
        this.authService = authService;
    }

    public List<ArkivertVedtak> hentVedtakFraArena(Fnr fnr) {
        authService.sjekkTilgangTilBrukerOgEnhet(TilgangType.SKRIVE, fnr);
        return hentArkiverteVedtakFraArena(fnr);
    }

    public byte[] hentVedtakPdf(String dokumentInfoId, String journalpostId) {
        // Tilgangskontroll gjøres av SAF
        return safClient.hentVedtakPdf(journalpostId, dokumentInfoId);
    }

    protected List<ArkivertVedtak> hentArkiverteVedtakFraArena(Fnr fnr) {
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
