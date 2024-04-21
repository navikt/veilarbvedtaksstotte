package no.nav.veilarbvedtaksstotte.client.registrering.request;

import no.nav.common.types.identer.Fnr;

public record RegistreringRequest(
        Fnr fnr,
        String behandlingsummer
) {
}
