package no.nav.veilarbvedtaksstotte.client.api;

import no.nav.common.health.HealthCheck;
import no.nav.veilarbvedtaksstotte.domain.DokumentSendtDTO;
import no.nav.veilarbvedtaksstotte.domain.SendDokumentDTO;

public interface DokumentClient extends HealthCheck {

    DokumentSendtDTO sendDokument(SendDokumentDTO sendDokumentDTO);

    byte[] produserDokumentUtkast(SendDokumentDTO sendDokumentDTO);

}
