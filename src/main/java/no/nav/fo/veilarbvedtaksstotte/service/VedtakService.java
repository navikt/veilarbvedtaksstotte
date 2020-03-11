package no.nav.fo.veilarbvedtaksstotte.service;

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

        AuthKontekst authKontekst = authService.sjekkTilgang(fnr);
        String aktorId = authKontekst.getAktorId();

        Vedtak vedtak = hentUtkastEllerFeil(aktorId);

        authService.sjekkAnsvarligVeileder(vedtak);

        Vedtak gjeldendeVedtak = vedtaksstotteRepository.hentGjeldendeVedtak(aktorId);
        validerUtkastForUtsending(vedtak, gjeldendeVedtak, beslutter);

        long vedtakId = vedtak.getId();

        oyeblikksbildeService.lagreOyeblikksbilde(fnr, vedtakId);

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

    public void lagUtkast(String fnr) {

        AuthKontekst authKontekst = authService.sjekkTilgang(fnr);
        String aktorId = authKontekst.getAktorId();
        Vedtak utkast = vedtaksstotteRepository.hentUtkast(aktorId);

        if (utkast != null) {
            throw new IllegalStateException(format("Kan ikke lage nytt utkast, bruker med aktorId %s har allerede et aktivt utkast", aktorId));
        }

        String veilederIdent = veilederService.hentVeilederIdentFraToken();
        String oppfolgingsenhetId = authKontekst.getOppfolgingsenhet();

        vedtaksstotteRepository.opprettUtkast(aktorId, veilederIdent, oppfolgingsenhetId);

        Vedtak opprettetUtkast = vedtaksstotteRepository.hentUtkast(aktorId);
        kafkaService.sendVedtakStatusEndring(opprettetUtkast, KafkaVedtakStatus.UTKAST_OPPRETTET);
    }

    public void oppdaterUtkast(String fnr, VedtakDTO vedtakDTO) {

        AuthKontekst authKontekst = authService.sjekkTilgang(fnr);

        Vedtak utkast = hentUtkastEllerFeil(authKontekst.getAktorId());

        authService.sjekkAnsvarligVeileder(utkast);

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

        String aktorId = authService.sjekkTilgang(fnr).getAktorId();
        Vedtak utkast = hentUtkastEllerFeil(aktorId);
        authService.sjekkAnsvarligVeileder(utkast);

        transactor.inTransaction(() -> {
            vedtaksstotteRepository.slettUtkast(aktorId);
            kilderRepository.slettKilder(utkast.getId());
        });

        kafkaService.sendVedtakStatusEndring(utkast, KafkaVedtakStatus.UTKAST_SLETTET);
        metricsService.rapporterUtkastSlettet();
    }

    public List<Vedtak> hentVedtak(String fnr) {

        String aktorId = authService.sjekkTilgang(fnr).getAktorId();

        List<Vedtak> vedtak = vedtaksstotteRepository.hentVedtak(aktorId);

        flettInnVeilederNavn(vedtak);
        flettInnEnhetNavn(vedtak);

        return vedtak;
    }

    public byte[] produserDokumentUtkast(String fnr) {

        String aktorId = authService.sjekkTilgang(fnr).getAktorId();

        SendDokumentDTO sendDokumentDTO =
                Optional.ofNullable(vedtaksstotteRepository.hentUtkast(aktorId))
                        .map(vedtak -> lagDokumentDTO(vedtak, fnr))
                        .orElseThrow(() -> new NotFoundException("Fant ikke vedtak å forhandsvise for bruker"));

        return dokumentClient.produserDokumentUtkast(sendDokumentDTO);
    }

    public byte[] hentVedtakPdf(String fnr, String dokumentInfoId, String journalpostId) {
        authService.sjekkTilgang(fnr);
        return safClient.hentVedtakPdf(journalpostId, dokumentInfoId);
    }

    public boolean harUtkast(String fnr) {
        String aktorId = authService.sjekkTilgang(fnr).getAktorId();
        return vedtaksstotteRepository.hentUtkast(aktorId) != null;
    }

    public void behandleAvsluttOppfolging (KafkaAvsluttOppfolging melding ) {
        String aktorId = melding.getAktorId();
        vedtaksstotteRepository.slettUtkast(aktorId);
        vedtaksstotteRepository.settGjeldendeVedtakTilHistorisk(aktorId);
    }

    public void behandleOppfolgingsbrukerEndring(KafkaOppfolgingsbrukerEndring endring) {
        Vedtak utkast = vedtaksstotteRepository.hentUtkast(endring.getAktorId());

        if (utkast != null && !utkast.getOppfolgingsenhetId().equals(endring.getOppfolgingsenhetId())) {
            utkast.setOppfolgingsenhetId(endring.getOppfolgingsenhetId());
            vedtaksstotteRepository.oppdaterUtkast(utkast.getId(), utkast);
        }
    }

    public void taOverUtkast(String fnr) {
        AuthKontekst authKontekst = authService.sjekkTilgang(fnr);

        Vedtak utkast = hentUtkastEllerFeil(authKontekst.getAktorId());

        String veilederId = veilederService.hentVeilederIdentFraToken();

        if (veilederId.equals(utkast.getVeilederIdent())) {
            throw new BadRequestException("Veileder er allerede ansvarlig for utkast");
        }

        utkast.setVeilederIdent(veilederId);

        vedtaksstotteRepository.oppdaterUtkast(utkast.getId(), utkast);
    }

    private Vedtak hentUtkastEllerFeil(String aktorId) {
        Vedtak utkast = vedtaksstotteRepository.hentUtkast(aktorId);

        if (utkast == null) {
            throw new NotFoundException("Fant ikke utkast");
        }

        return utkast;
    }

    private void flettInnVeilederNavn(List<Vedtak> vedtak) {
        vedtak.forEach(v -> {
            Veileder veileder = veilederService.hentVeileder(v.getVeilederIdent());
            v.setVeilederNavn(veileder != null ? veileder.getNavn() : null);
        });
    }

    private void flettInnEnhetNavn(List<Vedtak> vedtak) {
        vedtak.forEach(v -> {
            String enhetNavn = veilederService.hentEnhetNavn(v.getOppfolgingsenhetId());
            v.setOppfolgingsenhetNavn(enhetNavn);
        });
    }

    private SendDokumentDTO lagDokumentDTO(Vedtak vedtak, String fnr) {
        return new SendDokumentDTO()
                .setBegrunnelse(vedtak.getBegrunnelse())
                .setEnhetId(vedtak.getOppfolgingsenhetId())
                .setOpplysninger(vedtak.getOpplysninger())
                .setMalType(malTypeService.utledMalTypeFraVedtak(vedtak, fnr))
                .setBrukerFnr(fnr);
    }

    private boolean skalHaBeslutter(Innsatsgruppe innsatsgruppe) {
        return Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS == innsatsgruppe
                || Innsatsgruppe.VARIG_TILPASSET_INNSATS == innsatsgruppe;
    }

    void validerUtkastForUtsending(Vedtak vedtak, Vedtak gjeldendeVedtak, String beslutter) {

        Innsatsgruppe innsatsgruppe = vedtak.getInnsatsgruppe();

        if (innsatsgruppe == null) {
            throw new IllegalStateException("Vedtak mangler innsatsgruppe");
        }

        if (skalHaBeslutter(innsatsgruppe) && (beslutter == null || beslutter.isEmpty())) {
            throw new IllegalStateException("Vedtak kan ikke bli sendt uten beslutter");
        }

        if (vedtak.getOpplysninger() == null || vedtak.getOpplysninger().isEmpty()) {
            throw new IllegalStateException("Vedtak mangler opplysninger");
        }

        if (vedtak.getHovedmal() == null && innsatsgruppe != Innsatsgruppe.VARIG_TILPASSET_INNSATS) {
            throw new IllegalStateException("Vedtak mangler hovedmål");
        } else if (vedtak.getHovedmal() != null && innsatsgruppe == Innsatsgruppe.VARIG_TILPASSET_INNSATS) {
            throw new IllegalStateException("Vedtak med varig tilpasset innsats skal ikke ha hovedmål");
        }

        boolean harIkkeBegrunnelse = vedtak.getBegrunnelse() == null || vedtak.getBegrunnelse().trim().isEmpty();
        boolean erStandard = innsatsgruppe == Innsatsgruppe.STANDARD_INNSATS;
        boolean erGjeldendeVedtakVarig =
                gjeldendeVedtak != null &&
                        (gjeldendeVedtak.getInnsatsgruppe() == Innsatsgruppe.VARIG_TILPASSET_INNSATS ||
                                gjeldendeVedtak.getInnsatsgruppe() == Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS);

        if (harIkkeBegrunnelse && erStandard && erGjeldendeVedtakVarig) {
            throw new IllegalStateException("Vedtak mangler begrunnelse siden gjeldende vedtak er varig");
        } else if (harIkkeBegrunnelse && !erStandard) {
            throw new IllegalStateException("Vedtak mangler begrunnelse");
        }

    }

}
