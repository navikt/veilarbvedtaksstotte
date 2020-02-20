package no.nav.fo.veilarbvedtaksstotte.service;

import no.nav.apiapp.feil.IngenTilgang;
import no.nav.apiapp.security.veilarbabac.Bruker;
import no.nav.fo.veilarbvedtaksstotte.client.DokumentClient;
import no.nav.fo.veilarbvedtaksstotte.client.SAFClient;
import no.nav.fo.veilarbvedtaksstotte.domain.*;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.Innsatsgruppe;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.KafkaVedtakStatus;
import no.nav.fo.veilarbvedtaksstotte.repository.KilderRepository;
import no.nav.fo.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import no.nav.sbl.jdbc.Transactor;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static no.nav.fo.veilarbvedtaksstotte.utils.ValideringUtils.validerFnr;

@Service
public class VedtakService {

    private VedtaksstotteRepository vedtaksstotteRepository;
    private OyeblikksbildeService oyeblikksbildeService;
    private KilderRepository kilderRepository;
    private AuthService authService;
    private DokumentClient dokumentClient;
    private SAFClient safClient;
    private VeilederService veilederService;
    private MalTypeService malTypeService;
    private KafkaService kafkaService;
    private MetricsService metricsService;
    private Transactor transactor;

    @Inject
    public VedtakService(VedtaksstotteRepository vedtaksstotteRepository,
                         KilderRepository kilderRepository,
                         OyeblikksbildeService oyeblikksbildeService,
                         AuthService authService,
                         DokumentClient dokumentClient,
                         SAFClient safClient,
                         VeilederService veilederService,
                         MalTypeService malTypeService,
                         KafkaService kafkaService,
                         MetricsService metricsService, Transactor transactor) {
        this.vedtaksstotteRepository = vedtaksstotteRepository;
        this.kilderRepository = kilderRepository;
        this.oyeblikksbildeService = oyeblikksbildeService;
        this.authService = authService;
        this.dokumentClient = dokumentClient;
        this.safClient = safClient;
        this.veilederService = veilederService;
        this.malTypeService = malTypeService;
        this.kafkaService = kafkaService;
        this.metricsService = metricsService;
        this.transactor = transactor;
    }

    public DokumentSendtDTO sendVedtak(String fnr, String beslutter) {

        validerFnr(fnr);

        AuthKontekst authKontekst = authService.sjekkTilgang(fnr);
        String aktorId = authKontekst.getBruker().getAktoerId();

        Vedtak vedtak = hentUtkastEllerFeil(aktorId);

        sjekkAnsvarligVeileder(vedtak);

        if (skalHaBeslutter(vedtak.getInnsatsgruppe()) && (beslutter == null || beslutter.isEmpty())) {
            throw new IllegalStateException("Vedtak kan ikke bli sendt uten beslutter");
        }

        long vedtakId = vedtak.getId();

        oyeblikksbildeService.lagreOyeblikksbilde(fnr, vedtakId);

        sjekkOgOppdaterEnhet(vedtak, authKontekst.getOppfolgingsenhet());
        // TODO oppdater til ny status for "sender" + optimistic lock? Unngå potensielt å sende flere ganger
        vedtaksstotteRepository.oppdaterUtkast(vedtakId, vedtak);


        SendDokumentDTO sendDokumentDTO = lagDokumentDTO(vedtak, fnr);
        DokumentSendtDTO dokumentSendt = dokumentClient.sendDokument(sendDokumentDTO);

        transactor.inTransaction(() -> {
            vedtaksstotteRepository.settGjeldendeVedtakTilHistorisk(aktorId);
            vedtaksstotteRepository.ferdigstillVedtak(vedtakId, dokumentSendt, beslutter);
        });

        kafkaService.sendVedtak(vedtakId);
        kafkaService.sendVedtakStatusEndring(vedtak, KafkaVedtakStatus.SENDT_TIL_BRUKER);

        metricsService.rapporterVedtakSendt(vedtak);
        metricsService.rapporterTidFraRegistrering(vedtak, aktorId, fnr);
        metricsService.rapporterVedtakSendtSykmeldtUtenArbeidsgiver(vedtak, fnr);

        return dokumentSendt;
    }

    private Vedtak hentUtkastEllerFeil(String aktorId) {
        Vedtak utkast = vedtaksstotteRepository.hentUtkast(aktorId);

        if (utkast == null) {
            throw new NotFoundException("Fant ikke utkast");
        }

        return utkast;
    }

    private void sjekkAnsvarligVeileder(Vedtak vedtak) {
        if (!vedtak.getVeilederIdent().equals(veilederService.hentVeilederIdentFraToken())) {
            throw new IngenTilgang("Ikke ansvarlig saksbehandler.");
        }
    }

    private void sjekkOgOppdaterEnhet(Vedtak vedtak, String oppfolgingsenhetId) {
        if (!oppfolgingsenhetId.equals(vedtak.getVeilederEnhetId())) {
            String enhetNavn = veilederService.hentEnhetNavn(oppfolgingsenhetId);
            vedtak.setVeilederEnhetId(oppfolgingsenhetId);
            vedtak.setVeilederEnhetNavn(enhetNavn);
        }
    }

    public void lagUtkast(String fnr) {
        validerFnr(fnr);

        AuthKontekst authKontekst = authService.sjekkTilgang(fnr);
        String aktorId = authKontekst.getBruker().getAktoerId();
        Vedtak utkast = vedtaksstotteRepository.hentUtkast(aktorId);

        if (utkast != null) {
            throw new IllegalStateException(format("Kan ikke lage nytt utkast, bruker med aktorId %s har allerede et aktivt utkast", aktorId));
        }

        String veilederIdent = veilederService.hentVeilederIdentFraToken();
        String oppfolgingsenhetId = authKontekst.getOppfolgingsenhet();
        String enhetNavn = veilederService.hentEnhetNavn(oppfolgingsenhetId);

        vedtaksstotteRepository.opprettUtkast(aktorId, veilederIdent, oppfolgingsenhetId, enhetNavn);

        Vedtak opprettetUtkast = vedtaksstotteRepository.hentUtkast(aktorId);
        kafkaService.sendVedtakStatusEndring(opprettetUtkast, KafkaVedtakStatus.UTKAST_OPPRETTET);
    }

    public void oppdaterUtkast(String fnr, VedtakDTO vedtakDTO) {
        validerFnr(fnr);

        AuthKontekst authKontekst = authService.sjekkTilgang(fnr);

        Vedtak utkast = hentUtkastEllerFeil(authKontekst.getBruker().getAktoerId());

        sjekkAnsvarligVeileder(utkast);

        sjekkOgOppdaterEnhet(utkast, authKontekst.getOppfolgingsenhet());

        oppdaterUtkastFraDto(utkast, vedtakDTO);

        transactor.inTransaction(() -> {
            vedtaksstotteRepository.oppdaterUtkast(utkast.getId(), utkast);
            kilderRepository.slettKilder(utkast.getId());
            kilderRepository.lagKilder(vedtakDTO.getOpplysninger(), utkast.getId());
        });
    }

    private void oppdaterUtkastFraDto(Vedtak utkast, VedtakDTO dto) {
        utkast.setInnsatsgruppe(dto.getInnsatsgruppe());
        utkast.setBegrunnelse(dto.getBegrunnelse());
        utkast.setOpplysninger(dto.getOpplysninger());
        if (dto.getInnsatsgruppe() == Innsatsgruppe.VARIG_TILPASSET_INNSATS) {
            utkast.setHovedmal(null);
        } else {
            utkast.setHovedmal(dto.getHovedmal());
        }
    }

    public void slettUtkast(String fnr) {
        validerFnr(fnr);

        Bruker bruker = authService.sjekkTilgang(fnr).getBruker();
        Vedtak vedtak = vedtaksstotteRepository.hentUtkast(bruker.getAktoerId());

        transactor.inTransaction(() -> {
            vedtaksstotteRepository.slettUtkast(bruker.getAktoerId());
            kilderRepository.slettKilder(vedtak.getId());
        });

        kafkaService.sendVedtakStatusEndring(vedtak, KafkaVedtakStatus.UTKAST_SLETTET);
        metricsService.rapporterUtkastSlettet();
    }

    public List<Vedtak> hentVedtak(String fnr) {
        validerFnr(fnr);

        Bruker bruker = authService.sjekkTilgang(fnr).getBruker();

        List<Vedtak> vedtak = vedtaksstotteRepository.hentVedtak(bruker.getAktoerId());

        flettInnVeilederNavn(vedtak);

        return vedtak;
    }

    public byte[] produserDokumentUtkast(String fnr) {
        validerFnr(fnr);

        Bruker bruker = authService.sjekkTilgang(fnr).getBruker();

        SendDokumentDTO sendDokumentDTO =
                Optional.ofNullable(vedtaksstotteRepository.hentUtkast(bruker.getAktoerId()))
                        .map(vedtak -> lagDokumentDTO(vedtak, fnr))
                        .orElseThrow(() -> new NotFoundException("Fant ikke vedtak å forhandsvise for bruker"));

        return dokumentClient.produserDokumentUtkast(sendDokumentDTO);
    }

    public byte[] hentVedtakPdf(String fnr, String dokumentInfoId, String journalpostId) {
        authService.sjekkTilgang(fnr);
        return safClient.hentVedtakPdf(journalpostId, dokumentInfoId);
    }

    public boolean harUtkast(String fnr) {
        Bruker bruker = authService.sjekkTilgang(fnr).getBruker();
        return vedtaksstotteRepository.hentUtkast(bruker.getAktoerId()) != null;
    }

    public void behandleAvsluttOppfolging (KafkaAvsluttOppfolging melding ) {
        String aktorId = melding.getAktorId();
        vedtaksstotteRepository.slettUtkast(aktorId);
        vedtaksstotteRepository.settGjeldendeVedtakTilHistorisk(aktorId);
    }

    public void taOverUtkast(String fnr) {
        AuthKontekst authKontekst = authService.sjekkTilgang(fnr);

        Vedtak utkast = hentUtkastEllerFeil(authKontekst.getBruker().getAktoerId());

        String veilederId = veilederService.hentVeilederIdentFraToken();

        if (veilederId.equals(utkast.getVeilederIdent())) {
            throw new BadRequestException("Veileder er allerede ansvarlig for utkast");
        }

        utkast.setVeilederIdent(veilederId);
        sjekkOgOppdaterEnhet(utkast, authKontekst.getOppfolgingsenhet());

        vedtaksstotteRepository.oppdaterUtkast(utkast.getId(), utkast);
    }

    private void flettInnVeilederNavn(List<Vedtak> vedtak) {
        vedtak.forEach(v -> {
            Veileder veileder = veilederService.hentVeileder(v.getVeilederIdent());
            v.setVeilederNavn(veileder != null ? veileder.getNavn() : null);
        });
    }

    private SendDokumentDTO lagDokumentDTO(Vedtak vedtak, String fnr) {
        return new SendDokumentDTO()
                .setBegrunnelse(vedtak.getBegrunnelse())
                .setVeilederEnhet(vedtak.getVeilederEnhetId())
                .setOpplysninger(vedtak.getOpplysninger())
                .setMalType(malTypeService.utledMalTypeFraVedtak(vedtak, fnr))
                .setBrukerFnr(fnr);
    }

    private boolean skalHaBeslutter(Innsatsgruppe innsatsgruppe) {
        return Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS == innsatsgruppe
                || Innsatsgruppe.VARIG_TILPASSET_INNSATS == innsatsgruppe;
    }

}
