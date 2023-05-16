package no.nav.veilarbvedtaksstotte.service;

import no.nav.veilarbvedtaksstotte.client.egenvurdering.EgenvurderingClient;
import no.nav.veilarbvedtaksstotte.client.egenvurdering.EgenvurderingResponseDTO;
import no.nav.veilarbvedtaksstotte.client.person.VeilarbpersonClient;
import no.nav.veilarbvedtaksstotte.client.registrering.VeilarbregistreringClient;
import no.nav.veilarbvedtaksstotte.repository.OyeblikksbildeRepository;
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import org.joda.time.Instant;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class OyeblikksbildeServiceTest {
	private static final AuthService authService = mock(AuthService.class);
	private static final OyeblikksbildeRepository oyeblikksbildeRepository = mock(OyeblikksbildeRepository.class);
	private static final VedtaksstotteRepository vedtaksstotteRepository = mock(VedtaksstotteRepository.class);
	private static final VeilarbpersonClient veilarbpersonClient = mock(VeilarbpersonClient.class);
	private static final VeilarbregistreringClient registreringClient = mock(VeilarbregistreringClient.class);
	private static final EgenvurderingClient egenvurderingClient = mock(EgenvurderingClient.class);
	private static final OyeblikksbildeService oyeblikksbildeService = new OyeblikksbildeService(
		authService,
		oyeblikksbildeRepository,
		vedtaksstotteRepository,
		veilarbpersonClient,
		registreringClient,
		egenvurderingClient
	);
	@Test
	void mapEgenvurderingTilGammelStruktur() {
		Map<String,String> egenvurderingstekster = new HashMap<>();
		egenvurderingstekster.put("SITUASJONSBESTEMT_INNSATS", "Jeg vil få hjelp fra NAV");
		String egenvurderingDato = new Instant().toString();
		EgenvurderingResponseDTO egenvurdering = new EgenvurderingResponseDTO(egenvurderingDato, "dialog-123", "SITUASJONSBESTEMT_INNSATS", new EgenvurderingResponseDTO.Tekster("Ønsker du veiledning?", egenvurderingstekster));
		String egenvurderingJson = oyeblikksbildeService.mapEgenvurderingTilGammelStruktur(egenvurdering);

		String forventetEgenvurderingJson = "{\"sistOppdatert\":\""+egenvurderingDato+"\",\"svar\":[{\"spm\":\"Ønsker du veiledning?\",\"svar\":\"Jeg vil få hjelp fra NAV\",\"oppfolging\":\"SITUASJONSBESTEMT_INNSATS\",\"dialogId\":\"dialog-123\"}]}";
		assertEquals(forventetEgenvurderingJson, egenvurderingJson);
	}

	@Test
	void mapEgenvurderingTilGammelStruktur_med_null_argument() {

		String egenvurderingJson = oyeblikksbildeService.mapEgenvurderingTilGammelStruktur(null);

		String forventetEgenvurderingJson = "{\"ingenData\":\"Bruker har ikke fylt ut egenvurdering\"}";
		assertEquals(forventetEgenvurderingJson, egenvurderingJson);
	}

	@Test
	void mapEgenvurderingTilGammelStruktur_ikke_alle_verdier_satt() {
		Map<String,String> egenvurderingstekster = new HashMap<>();
		egenvurderingstekster.put("SITUASJONSBESTEMT_INNSATS", "Jeg vil få hjelp fra NAV");
		EgenvurderingResponseDTO egenvurdering = new EgenvurderingResponseDTO(null, null, "SITUASJONSBESTEMT_INNSATS", new EgenvurderingResponseDTO.Tekster("Ønsker du veiledning?", egenvurderingstekster));
		String egenvurderingJson = oyeblikksbildeService.mapEgenvurderingTilGammelStruktur(egenvurdering);

		String forventetEgenvurderingJson = "{\"sistOppdatert\":null,\"svar\":[{\"spm\":\"Ønsker du veiledning?\",\"svar\":\"Jeg vil få hjelp fra NAV\",\"oppfolging\":\"SITUASJONSBESTEMT_INNSATS\",\"dialogId\":null}]}";
		assertEquals(forventetEgenvurderingJson, egenvurderingJson);
	}

}
