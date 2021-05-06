package no.nav.veilarbvedtaksstotte.client.dokarkiv;

import no.nav.common.health.HealthCheck;
import no.nav.common.types.identer.Fnr;

import java.util.List;

public interface SafClient extends HealthCheck {

     byte[] hentVedtakPdf(String journalpostId, String dokumentInfoId);

     List<Journalpost> hentJournalposter(Fnr fnr);

}
