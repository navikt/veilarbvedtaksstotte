package no.nav.veilarbvedtaksstotte.client.api.dokarkiv;

import no.nav.common.health.HealthCheck;

import java.util.List;

public interface SafClient extends HealthCheck {

     byte[] hentVedtakPdf(String journalpostId, String dokumentInfoId);

     List<Journalpost> hentJournalposter(String fnr);

}
