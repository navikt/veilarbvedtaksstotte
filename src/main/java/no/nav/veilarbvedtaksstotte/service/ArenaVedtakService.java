package no.nav.veilarbvedtaksstotte.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.Fnr;
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

@Slf4j
@Service
public class ArenaVedtakService {

    final static String JOURNALPOST_ARENA_VEDTAK_TITTEL = "Brev: Oppfølgingsvedtak (§14a)";
    final static String MODIA_REG_USER = "MODIA";

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

    public List<ArkivertVedtak> hentVedtakFraArena(Fnr fnr) {
        authService.sjekkTilgangTilBrukerOgEnhet(fnr);
        return hentArkiverteVedtakFraArena(fnr);
    }

    public byte[] hentVedtakPdf(String dokumentInfoId, String journalpostId) {
        // Tilgangskontroll gjøres av SAF
        return safClient.hentVedtakPdf(journalpostId, dokumentInfoId);
    }

    /**
     * @param arenaVedtak Kafka-melding om vedtak fra Arena
     * @return true dersom behandling av Kafka-melding fører til lagring/oppdatering i databasen
     *
     * Idempotent behandling av Kafka-melding om vedtak fra Arena. Lagrer kun siste vedtak per fnr.
     * For minst mulig logikk så:
     * - Tas det her ikke høyde for endring av fnr, dvs lagring per fnr og ikke per bruker
     * - Lagrer siste vedtak fra Arena selv om det finnes et nyere vedtak i denne løsningen
     */
    public Boolean behandleVedtakFraArena(ArenaVedtak arenaVedtak) {

        if (MODIA_REG_USER.equals(arenaVedtak.getRegUser())) {
            return false;
        }

        ArenaVedtak eksisterendeVedtak = arenaVedtakRepository.hentVedtak(arenaVedtak.getFnr());

        if (eksisterendeVedtak != null && (
                eksisterendeVedtak.getHendelseId() == arenaVedtak.getHendelseId() ||
                        eksisterendeVedtak.beregnetFattetTidspunkt().isAfter(arenaVedtak.beregnetFattetTidspunkt())
        )
        ) {
            return false;
        }

        arenaVedtakRepository.upsertVedtak(arenaVedtak);

        return true;
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
