package no.nav.fo.veilarbvedtaksstotte.service;

import no.nav.fo.veilarbvedtaksstotte.client.OppgaveClient;
import no.nav.fo.veilarbvedtaksstotte.client.VeiledereOgEnhetClient;
import no.nav.fo.veilarbvedtaksstotte.domain.*;
import no.nav.fo.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

import java.util.Optional;

import static no.nav.fo.veilarbvedtaksstotte.utils.ValideringUtils.validerFnr;

@Service
public class BeslutterOppgaveService {

	private final static String TEMA_OPPFOLGING = "OPP";
	private final static String BEHANDLINGSTYPE_TIL_BESLUTTER = "ae0229";
	private final static String OPPGAVETYPE_VURDER_HENDVENDELSE = "VURD_HENV";

	private AuthService authService;
	private OppgaveClient oppgaveClient;
	private VeiledereOgEnhetClient veiledereOgEnhetClient;
	private VedtaksstotteRepository vedtaksstotteRepository;

	@Inject
	public BeslutterOppgaveService(
			AuthService authService,
			OppgaveClient oppgaveClient,
			VeiledereOgEnhetClient veiledereOgEnhetClient, VedtaksstotteRepository vedtaksstotteRepository
	) {
		this.authService = authService;
		this.oppgaveClient = oppgaveClient;
		this.veiledereOgEnhetClient = veiledereOgEnhetClient;
		this.vedtaksstotteRepository = vedtaksstotteRepository;
	}

	public void sendBeslutterOppgave(SendBeslutterOppgaveDTO sendBeslutterOppgaveDTO, String fnr) {
		validerFnr(fnr);

		AuthKontekst authKontekst = authService.sjekkTilgang(fnr);
		String aktorId = authKontekst.getBruker().getAktoerId();
		Vedtak utkast = vedtaksstotteRepository.hentUtkast(aktorId);

		if (utkast.isSendtTilBeslutter()) {
			throw new RuntimeException("Kan ikke sende mer enn en oppgave til beslutter per utkast");
		}

		String beslutterIdent = sendBeslutterOppgaveDTO.getBeslutterIdent();
		String beslutterNavn = null;

		if (beslutterIdent != null) {
			VeilederePaEnhetDTO veiledere = veiledereOgEnhetClient.hentVeilederePaEnhet(utkast.getVeilederEnhetId());
			Optional<Veileder> beslutterFraEnhet = veiledere.getVeilederListe().stream()
					.filter((v) -> v.getIdent().equalsIgnoreCase(beslutterIdent))
					.findFirst();

			beslutterNavn = beslutterFraEnhet
					.orElseThrow(() -> new RuntimeException("Beslutter må være på samme enhet som vedtaket tilhører"))
					.getNavn();
		}

		OpprettOppgaveDTO opprettOppgaveDTO = mapTilOpprettOppgaveDTO(sendBeslutterOppgaveDTO)
				.setAktoerId(aktorId)
				.setTildeltEnhetsnr(utkast.getVeilederEnhetId())
				.setOpprettetAvEnhetsnr(utkast.getVeilederEnhetId())
				.setTilordnetRessurs(beslutterIdent)
				.setTema(TEMA_OPPFOLGING)
				.setBehandlingstype(BEHANDLINGSTYPE_TIL_BESLUTTER)
				.setOppgavetype(OPPGAVETYPE_VURDER_HENDVENDELSE);

		oppgaveClient.opprettOppgave(opprettOppgaveDTO);
		vedtaksstotteRepository.markerUtkastSomSendtTilBeslutter(aktorId, beslutterNavn);
	}

	private static OpprettOppgaveDTO mapTilOpprettOppgaveDTO(SendBeslutterOppgaveDTO beslutterOppgaveDTO) {
		return new OpprettOppgaveDTO()
				.setBeskrivelse(beslutterOppgaveDTO.getBeskrivelse())
				.setAktivDato(beslutterOppgaveDTO.getAktivFra())
				.setFristFerdigstillelse(beslutterOppgaveDTO.getFrist())
				.setTilordnetRessurs(beslutterOppgaveDTO.getBeslutterIdent())
				.setPrioritet(beslutterOppgaveDTO.getPrioritet());
	}

}
