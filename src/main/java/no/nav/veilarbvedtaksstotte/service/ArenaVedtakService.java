package no.nav.veilarbvedtaksstotte.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.SafClient;
import no.nav.veilarbvedtaksstotte.domain.arkiv.ArkivertVedtak;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.Journalpost;
import no.nav.veilarbvedtaksstotte.domain.vedtak.ArenaVedtak;
import no.nav.veilarbvedtaksstotte.kafka.dto.KafkaAvsluttOppfolging;
import no.nav.veilarbvedtaksstotte.repository.ArenaVedtakRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Slf4j
@Service
public class ArenaVedtakService {

    final static String JOURNALPOST_ARENA_VEDTAK_TITTEL = "Brev: Oppfølgingsvedtak (§14a)";
    final static String MODIA_REG_USER = "MODIA";

    private ArenaVedtakRepository arenaVedtakRepository;
    private SafClient safClient;
    private AuthService authService;
    private AktorOppslagClient aktorOppslagClient;

    @Autowired
    public ArenaVedtakService(ArenaVedtakRepository arenaVedtakRepository,
                              SafClient safClient,
                              AuthService authService,
                              AktorOppslagClient aktorOppslagClient) {
        this.arenaVedtakRepository = arenaVedtakRepository;
        this.safClient = safClient;
        this.authService = authService;
        this.aktorOppslagClient = aktorOppslagClient;
    }

    public List<ArkivertVedtak> hentVedtakFraArena(String fnr) {
        authService.sjekkTilgangTilFnr(fnr);
        return hentArkiverteVedtakFraArena(fnr);
    }

    public byte[] hentVedtakPdf(String dokumentInfoId, String journalpostId) {
        // Tilgangskontroll gjøres av SAF
        return safClient.hentVedtakPdf(journalpostId, dokumentInfoId);
    }

    /**
     * @param arenaVedtak Kafka-melding om vedtak fra Arena
     *
     * Behandling av Kafka-melding om vedtak fra Arena. Lagrer kun siste vedtak per fnr. For minst mulig logikk og
     * for å unngå oppslag i andre tjenester i konsumering av Kafka-melding så:
     *  - Tas det ikke høyde for endring av fnr her, dvs lagring per fnr og ikke per bruker
     *  - Lagrer siste vedtak fra Arena selv om det finnes et nyere vedtak i denne løsningen
     */
    public void behandleVedtakFraArena(ArenaVedtak arenaVedtak) {

        if (MODIA_REG_USER.equals(arenaVedtak.getRegUser())) {
            return;
        }

        ArenaVedtak eksisterendeVedtak = arenaVedtakRepository.hentVedtak(arenaVedtak.getFnr());

        if (eksisterendeVedtak != null && !arenaVedtak.getFraDato().isAfter(eksisterendeVedtak.getFraDato())) {
            return;
        }

        arenaVedtakRepository.upsertVedtak(arenaVedtak);
    }

    public void behandleAvsluttOppfolging(KafkaAvsluttOppfolging melding) {
        slettArenaVedtakKopi(AktorId.of(melding.getAktorId()));
    }

    public void slettArenaVedtakKopi(AktorId aktorId) {
        List<Fnr> fnr = List.of(aktorOppslagClient.hentFnr(aktorId)); // TODO hent alle identer
        int antall = arenaVedtakRepository.slettVedtak(fnr);
        if (antall > 0) {
            log.info(format("Slettet %s kopi(er) av vedtak fra Arena for aktorId=%s",
                    antall, aktorId));
        }
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
