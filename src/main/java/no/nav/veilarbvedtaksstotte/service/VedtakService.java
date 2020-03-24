package no.nav.veilarbvedtaksstotte.service;

import lombok.SneakyThrows;
import no.nav.veilarbvedtaksstotte.client.DokumentClient;
import no.nav.veilarbvedtaksstotte.client.SAFClient;
import no.nav.veilarbvedtaksstotte.domain.enums.Innsatsgruppe;
import no.nav.veilarbvedtaksstotte.domain.enums.KafkaVedtakStatus;
import no.nav.veilarbvedtaksstotte.repository.KilderRepository;
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import no.nav.sbl.jdbc.Transactor;
import no.nav.veilarbvedtaksstotte.domain.*;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static no.nav.veilarbvedtaksstotte.utils.InnsatsgruppeUtils.skalHaBeslutter;

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
                         MetricsService metricsService,
                         Transactor transactor) {
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

    @SneakyThrows
    public DokumentSendtDTO sendVedtak(String fnr) {

        AuthKontekst authKontekst = authService.sjekkTilgang(fnr);
        String aktorId = authKontekst.getAktorId();

        Vedtak vedtak = vedtaksstotteRepository.hentUtkastEllerFeil(aktorId);

        authService.sjekkAnsvarligVeileder(vedtak);

        Vedtak gjeldendeVedtak = vedtaksstotteRepository.hentGjeldendeVedtak(aktorId);
        validerUtkastForUtsending(vedtak, gjeldendeVedtak);

        long vedtakId = vedtak.getId();

        oyeblikksbildeService.lagreOyeblikksbilde(fnr, vedtakId);

        DokumentSendtDTO dokumentSendt = sendDokument(vedtak, fnr);

        transactor.inTransaction(() -> {
            vedtaksstotteRepository.settGjeldendeVedtakTilHistorisk(aktorId);
            vedtaksstotteRepository.ferdigstillVedtak(vedtakId, dokumentSendt);
        });

        sendKafkaMeldingerForVedtakSendt(vedtak);

        rapporterMetrikkerForVedtakSendt(vedtak, fnr);

        return dokumentSendt;
    }

    private DokumentSendtDTO sendDokument(Vedtak vedtak, String fnr) {
        // Oppdaterer vedtak til "sender" tilstand for å redusere risiko for dupliserte utsendelser av dokument.
        vedtaksstotteRepository.oppdaterSender(vedtak.getId(), true);
        DokumentSendtDTO dokumentSendt;
        try {
            SendDokumentDTO sendDokumentDTO = lagDokumentDTO(vedtak, fnr);

            dokumentSendt = dokumentClient.sendDokument(sendDokumentDTO);
        } catch (Exception e) {
            vedtaksstotteRepository.oppdaterSender(vedtak.getId(), false);
            throw e;
        }
        return dokumentSendt;
    }

    private void sendKafkaMeldingerForVedtakSendt(Vedtak vedtak) {
        kafkaService.sendVedtak(vedtak.getId());
        kafkaService.sendVedtakStatusEndring(vedtak, KafkaVedtakStatus.SENDT_TIL_BRUKER);
    }

    private void rapporterMetrikkerForVedtakSendt(Vedtak vedtak, String fnr) {
        metricsService.rapporterVedtakSendt(vedtak);
        metricsService.rapporterTidFraRegistrering(vedtak, vedtak.getAktorId(), fnr);
        metricsService.rapporterVedtakSendtSykmeldtUtenArbeidsgiver(vedtak, fnr);
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

        Vedtak utkast = vedtaksstotteRepository.hentUtkastEllerFeil(authKontekst.getAktorId());

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
        Vedtak utkast = vedtaksstotteRepository.hentUtkastEllerFeil(aktorId);
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
        flettInnBeslutterNavn(vedtak);
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

    public void taOverUtkast(String fnr, String taOverVedtakFor) {
        AuthKontekst authKontekst = authService.sjekkTilgang(fnr);

        Vedtak utkast = vedtaksstotteRepository.hentUtkastEllerFeil(authKontekst.getAktorId());

        String veilederId = veilederService.hentVeilederIdentFraToken();

        if (veilederId.equals(utkast.getVeilederIdent())) {
            throw new BadRequestException("Veileder er allerede ansvarlig for utkast");
        }

        if (taOverVedtakFor.equals("veileder")) {
            utkast.setVeilederIdent(veilederId);
        } else if (taOverVedtakFor.equals("beslutter")){
            utkast.setBeslutterIdent(veilederId);
        }
                                      
        vedtaksstotteRepository.oppdaterUtkast(utkast.getId(), utkast);
    }

    private void flettInnVeilederNavn(List<Vedtak> vedtak) {
        vedtak.forEach(v -> {
            Veileder veileder = veilederService.hentVeileder(v.getVeilederIdent());
            v.setVeilederNavn(veileder != null ? veileder.getNavn() : null);
        });
    }

    private void flettInnBeslutterNavn(List<Vedtak> vedtak) {
        vedtak.stream()
                .filter(v -> v.getBeslutterIdent() != null)
                .forEach(v -> {
                    Veileder beslutter = veilederService.hentVeileder(v.getBeslutterIdent());
                    v.setBeslutterNavn(beslutter != null ? beslutter.getNavn() : null);
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

    void validerUtkastForUtsending(Vedtak vedtak, Vedtak gjeldendeVedtak) {

        Innsatsgruppe innsatsgruppe = vedtak.getInnsatsgruppe();

        if (innsatsgruppe == null) {
            throw new IllegalStateException("Vedtak mangler innsatsgruppe");
        }

        if (skalHaBeslutter(innsatsgruppe) || vedtak.isBeslutterProsessStartet()) {
            if (vedtak.getBeslutterIdent() == null) {
                throw new IllegalStateException("Vedtak kan ikke bli sendt uten beslutter");
            } else if (!vedtak.isGodkjentAvBeslutter()) {
                throw new IllegalStateException("Vedtak er ikke godkjent av beslutter");
            }
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
