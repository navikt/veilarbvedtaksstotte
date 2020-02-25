package no.nav.fo.veilarbvedtaksstotte.service;

import no.nav.fo.veilarbvedtaksstotte.client.OppfolgingClient;
import no.nav.fo.veilarbvedtaksstotte.client.SAFClient;
import no.nav.fo.veilarbvedtaksstotte.domain.ArkivertVedtak;
import no.nav.fo.veilarbvedtaksstotte.domain.AuthKontekst;
import no.nav.fo.veilarbvedtaksstotte.domain.Journalpost;
import no.nav.fo.veilarbvedtaksstotte.domain.OppfolgingDTO;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.Innsatsgruppe;
import no.nav.fo.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import no.nav.fo.veilarbvedtaksstotte.utils.OppfolgingUtils;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ArenaVedtakService {

    final static String JOURNALPOST_ARENA_VEDTAK_TITTEL = "Brev: Oppfølgingsvedtak (§14a)";

    private VedtaksstotteRepository vedtaksstotteRepository;
    private SAFClient safClient;
    private AuthService authService;
    private OppfolgingClient oppfolgingClient;

    @Inject
    public ArenaVedtakService(VedtaksstotteRepository vedtaksstotteRepository, SAFClient safClient, AuthService authService, OppfolgingClient oppfolgingClient) {
        this.vedtaksstotteRepository = vedtaksstotteRepository;
        this.safClient = safClient;
        this.authService = authService;
        this.oppfolgingClient = oppfolgingClient;
    }

    public List<ArkivertVedtak> hentVedtakFraArena(String fnr) {
        AuthKontekst authKontekst = authService.sjekkTilgang(fnr);
        String aktorId = authKontekst.getAktorId();

        List<ArkivertVedtak> vedtakFraArena = hentArkiverteVedtakFraArena(fnr);
        Optional<ArkivertVedtak> kanskjeGjeldendeVedtak = finnGjeldendeVedtakFraArena(vedtakFraArena, fnr, aktorId);

        if (kanskjeGjeldendeVedtak.isPresent()) {
            OppfolgingDTO oppfolgingData = oppfolgingClient.hentOppfolgingData(fnr);
            Innsatsgruppe gjeldendeInnsatsgruppe = OppfolgingUtils.utledInnsatsgruppe(oppfolgingData.getServicegruppe());

            ArkivertVedtak gjeldendeVedtak = kanskjeGjeldendeVedtak.get();
            gjeldendeVedtak.erGjeldende = true;
            gjeldendeVedtak.gjeldendeInnsatsgruppe = gjeldendeInnsatsgruppe;
        }

        return vedtakFraArena;
    }

    protected Optional<ArkivertVedtak> finnGjeldendeVedtakFraArena(List<ArkivertVedtak> vedtakFraArena, String fnr, String aktorId) {
        Optional<ArkivertVedtak> kanskjeSisteVedtak = finnSisteArkiverteVedtak(vedtakFraArena);
        boolean harGjeldendeVedtak = vedtaksstotteRepository.harGjeldendeVedtak(aktorId);

        if (!kanskjeSisteVedtak.isPresent() || harGjeldendeVedtak) {
            return Optional.empty();
        }

        ArkivertVedtak sisteVedtak = kanskjeSisteVedtak.get();
        OppfolgingDTO oppfolgingData = oppfolgingClient.hentOppfolgingData(fnr);
        Optional<LocalDateTime> oppfolgingStartDato = OppfolgingUtils.getOppfolgingStartDato(oppfolgingData.getOppfolgingsPerioder());

        boolean erSisteVedtakFraArenaGjeldende = oppfolgingStartDato.isPresent()
                && sisteVedtak.datoOpprettet.isAfter(oppfolgingStartDato.get());

        return Optional.ofNullable(erSisteVedtakFraArenaGjeldende ? sisteVedtak : null);
    }

    protected List<ArkivertVedtak> hentArkiverteVedtakFraArena(String fnr) {
        return safClient.hentJournalposter(fnr)
                .stream()
                .filter(this::erVedtakFraArena)
                .map(this::tilArkivertVedtak)
                .filter(this::harDokumentInfoId)
                .collect(Collectors.toList());
    }

    static Optional<ArkivertVedtak> finnSisteArkiverteVedtak(List<ArkivertVedtak> arkivertVedtak) {
        LocalDateTime sisteDato = LocalDateTime.MIN;
        ArkivertVedtak sisteVedtak = null;

        for (ArkivertVedtak vedtak : arkivertVedtak) {
            if (vedtak.datoOpprettet.isAfter(sisteDato)) {
                sisteDato = vedtak.datoOpprettet;
                sisteVedtak = vedtak;
            }
        }

        return Optional.ofNullable(sisteVedtak);
    }

    private ArkivertVedtak tilArkivertVedtak(Journalpost journalpost) {
        ArkivertVedtak arkivertVedtak = new ArkivertVedtak();

        arkivertVedtak.journalpostId = journalpost.journalpostId;
        arkivertVedtak.journalforendeEnhet = journalpost.journalforendeEnhet;
        arkivertVedtak.journalfortAvNavn = journalpost.journalfortAvNavn;
        arkivertVedtak.datoOpprettet = LocalDateTime.parse(journalpost.datoOpprettet);

        if (journalpost.dokumenter != null && journalpost.dokumenter.length > 0) {
            arkivertVedtak.dokumentInfoId = journalpost.dokumenter[0].dokumentInfoId;
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
