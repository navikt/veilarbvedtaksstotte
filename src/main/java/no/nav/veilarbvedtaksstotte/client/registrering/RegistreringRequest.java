package no.nav.veilarbvedtaksstotte.client.registrering;

import no.nav.common.types.identer.Fnr;

public record RegistreringRequest(
    Fnr fnr
) {
}
