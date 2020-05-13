package no.nav.veilarbvedtaksstotte.domain.dialog;

import lombok.Data;
import lombok.experimental.Accessors;

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

        melding.systemMeldingType = systemMelding.systemMeldingType;
        melding.opprettet = systemMelding.opprettet;
        melding.utfortAvIdent = systemMelding.utfortAvIdent;

        return melding;
    }
}
