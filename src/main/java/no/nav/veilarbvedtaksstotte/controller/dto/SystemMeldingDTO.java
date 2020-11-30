package no.nav.veilarbvedtaksstotte.controller.dto;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.veilarbvedtaksstotte.domain.dialog.SystemMelding;
import no.nav.veilarbvedtaksstotte.domain.dialog.SystemMeldingType;

@Data
@Accessors(chain = true)
public class SystemMeldingDTO extends MeldingDTO {
    SystemMeldingType systemMeldingType;
    String utfortAvNavn;
    String utfortAvIdent;

    public SystemMeldingDTO() {
        type = MeldingType.SYSTEM_MELDING;
    }

    public static SystemMeldingDTO fraMelding(SystemMelding systemMelding) {
        SystemMeldingDTO melding = new SystemMeldingDTO();

        melding.systemMeldingType = systemMelding.getSystemMeldingType();
        melding.opprettet = systemMelding.getOpprettet();
        melding.utfortAvIdent = systemMelding.getUtfortAvIdent();

        return melding;
    }
}
