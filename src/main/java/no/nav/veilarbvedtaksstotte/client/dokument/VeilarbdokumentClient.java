package no.nav.veilarbvedtaksstotte.client.dokument;

import no.nav.common.health.HealthCheck;

public interface VeilarbdokumentClient extends HealthCheck {

    DokumentSendtDTO sendDokument(SendDokumentDTO sendDokumentDTO);

    byte[] produserDokumentUtkast(SendDokumentDTO sendDokumentDTO);

}
