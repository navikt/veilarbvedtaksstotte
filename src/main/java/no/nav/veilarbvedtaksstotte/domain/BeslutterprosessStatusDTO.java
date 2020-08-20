package no.nav.veilarbvedtaksstotte.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import no.nav.veilarbvedtaksstotte.domain.enums.BeslutterProsessStatus;

@Data
@AllArgsConstructor
public class BeslutterprosessStatusDTO {
    BeslutterProsessStatus status;
}
