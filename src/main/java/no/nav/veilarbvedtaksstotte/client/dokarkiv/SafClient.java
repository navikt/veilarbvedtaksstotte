package no.nav.veilarbvedtaksstotte.client.dokarkiv;

import no.nav.common.health.HealthCheck;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.dto.Journalpost;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.dto.JournalpostGraphqlResponse;

import java.util.List;

public interface SafClient extends HealthCheck {

    byte[] hentVedtakPdf(String journalpostId, String dokumentInfoId);

    List<Journalpost> hentJournalposter(Fnr fnr);

    JournalpostGraphqlResponse hentJournalpost(String journalpostId);

}
