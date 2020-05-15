package no.nav.veilarbvedtaksstotte.client.api;

import no.nav.common.health.HealthCheck;
import no.nav.veilarbvedtaksstotte.domain.Journalpost;

import java.util.List;

public interface SafClient extends HealthCheck {

     byte[] hentVedtakPdf(String journalpostId, String dokumentInfoId);

     List<Journalpost> hentJournalposter(String fnr);

}
