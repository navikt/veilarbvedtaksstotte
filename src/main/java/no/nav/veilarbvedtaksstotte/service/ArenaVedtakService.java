package no.nav.veilarbvedtaksstotte.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.Fnr;
import no.nav.poao_tilgang.client.TilgangType;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.SafClient;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.dto.Journalpost;
import no.nav.veilarbvedtaksstotte.domain.arkiv.ArkivertVedtak;
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

    private final ArenaVedtakRepository arenaVedtakRepository;
    private final SafClient safClient;
    private final AuthService authService;

    @Autowired
    public ArenaVedtakService(ArenaVedtakRepository arenaVedtakRepository,
                              SafClient safClient,
                              AuthService authService) {
        this.arenaVedtakRepository = arenaVedtakRepository;
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

    /**
     * Idempotent behandling av Kafka-melding om vedtak fra Arena. Lagrer kun siste vedtak per fnr.
     * For minst mulig logikk så:
     * - Tas det her ikke høyde for endring av fnr, dvs lagring per fnr og ikke per bruker
     * - Lagrer siste vedtak fra Arena selv om det finnes et nyere vedtak i denne løsningen
     *
     * @param arenaVedtak Kafka-melding om vedtak fra Arena
     * @return true dersom behandling av Kafka-melding fører til lagring/oppdatering i databasen
     */
    public Boolean behandleVedtakFraArena(ArenaVedtak arenaVedtak) {
        if (MODIA_REG_USER.equals(arenaVedtak.getRegUser())) {
            log.info("Behandler ikke melding om vedtak fra Arena med hendelsesId={} og fraDato={}. " +
                            "Årsak: vedtaket er fattet i ny vedtaksløsning i Modia arbeidsrettet oppfølging (dvs. regUser={}) og er allerede behandlet.",
                    arenaVedtak.getHendelseId(),
                    arenaVedtak.getFraDato(),
                    MODIA_REG_USER
            );
            return false;
        }

        ArenaVedtak eksisterendeVedtak = arenaVedtakRepository.hentVedtak(arenaVedtak.getFnr());

        if (eksisterendeVedtak != null &&
                eksisterendeVedtak.getHendelseId() >= arenaVedtak.getHendelseId()
        ) {
            log.info("Behandler ikke melding om vedtak fra Arena med hendelseId={} og fraDato={}. " +
                            "Årsak: vedtak med samme hendelseId er allerede lagret på personen.",
                    arenaVedtak.getHendelseId(),
                    arenaVedtak.getFraDato()
            );
            return false;
        }

        String vedtakLagretMelding = String.format("Lagret nytt vedtak fra Arena med hendelseId=%s.", arenaVedtak.getHendelseId());
        String eksisterendeVedtakMelding = eksisterendeVedtak != null ? String.format("Overskrev eksisterende lagret vedtak fra Arena for samme person med hendelseId=%s.", eksisterendeVedtak.getHendelseId()) : "";
        log.info("{} {}", vedtakLagretMelding, eksisterendeVedtakMelding);
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
