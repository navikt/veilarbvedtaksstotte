package no.nav.fo.veilarbvedtaksstotte.service;

import no.nav.fo.veilarbvedtaksstotte.client.OppgaveClient;
import no.nav.fo.veilarbvedtaksstotte.domain.AuthKontekst;
import no.nav.fo.veilarbvedtaksstotte.domain.OpprettOppgaveDTO;
import no.nav.fo.veilarbvedtaksstotte.domain.SendBeslutterOppgaveDTO;
import no.nav.fo.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

import static no.nav.fo.veilarbvedtaksstotte.utils.ValideringUtils.validerFnr;

@Service
public class BeslutterOppgaveService {

	private final static String TEMA_OPPFOLGING = "OPP";
	private final static String BEHANDLINGSTYPE_TIL_BESLUTTER = "ae0229";
	private final static String OPPGAVETYPE_VURDER_HENDVENDELSE = "VURD_HENV";

	private AuthService authService;
	private OppgaveClient oppgaveClient;
	private VedtaksstotteRepository vedtaksstotteRepository;

	@Inject
	public BeslutterOppgaveService(
			AuthService authService,
			OppgaveClient oppgaveClient,
			VedtaksstotteRepository vedtaksstotteRepository
	) {
		this.authService = authService;
		this.oppgaveClient = oppgaveClient;
		this.vedtaksstotteRepository = vedtaksstotteRepository;
	}

	public void sendBeslutterOppgave(SendBeslutterOppgaveDTO sendBeslutterOppgaveDTO, String fnr) {
		validerFnr(fnr);

		AuthKontekst authKontekst = authService.sjekkTilgang(fnr);
		String aktorId = authKontekst.getBruker().getAktoerId();

		if (vedtaksstotteRepository.harSendtUtkastTilBeslutter(aktorId)) {
			throw new RuntimeException("Kan ikke sende mer enn en oppgave til beslutter per utkast");
		}

		OpprettOppgaveDTO opprettOppgaveDTO = mapTilOpprettOppgaveDTO(sendBeslutterOppgaveDTO)
				.setAktoerId(aktorId)
				.setTema(TEMA_OPPFOLGING)
				.setBehandlingstype(BEHANDLINGSTYPE_TIL_BESLUTTER)
				.setOppgavetype(OPPGAVETYPE_VURDER_HENDVENDELSE);

		oppgaveClient.opprettOppgave(opprettOppgaveDTO);
		vedtaksstotteRepository.markerUtkastSomSendtTilBeslutter(aktorId, sendBeslutterOppgaveDTO.getBeslutterNavn());
	}

	private static OpprettOppgaveDTO mapTilOpprettOppgaveDTO(SendBeslutterOppgaveDTO beslutterOppgaveDTO) {
		return new OpprettOppgaveDTO()
				.setTildeltEnhetsnr(beslutterOppgaveDTO.getEnhetId())
				.setOpprettetAvEnhetsnr(beslutterOppgaveDTO.getEnhetId())
				.setBeskrivelse(beslutterOppgaveDTO.getBeskrivelse())
				.setAktivDato(beslutterOppgaveDTO.getAktivFra())
				.setFristFerdigstillelse(beslutterOppgaveDTO.getFrist())
				.setTilordnetRessurs(beslutterOppgaveDTO.getBeslutterIdent())
				.setPrioritet(beslutterOppgaveDTO.getPrioritet());
	}

}
