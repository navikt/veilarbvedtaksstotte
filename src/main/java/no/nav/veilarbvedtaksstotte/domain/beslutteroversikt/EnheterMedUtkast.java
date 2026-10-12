package no.nav.veilarbvedtaksstotte.domain.beslutteroversikt;

import io.swagger.v3.oas.annotations.media.Schema;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.dto.EnhetNavn;

import java.util.List;

public record EnheterMedUtkast(
        @Schema(description = "Enheter med utkast som innlogget veileder har enhetstilgang til.")
        List<EnhetNavn> medTilgang,
        @Schema(description = "Enheter med utkast som innlogget veileder ikke har enhetstilgang til.")
        List<EnhetNavn> utenTilgang
) {
}
