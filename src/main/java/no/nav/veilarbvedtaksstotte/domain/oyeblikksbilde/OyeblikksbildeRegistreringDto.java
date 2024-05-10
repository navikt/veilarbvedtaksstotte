package no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import no.nav.veilarbvedtaksstotte.client.registrering.dto.RegistreringResponseDto;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class OyeblikksbildeRegistreringDto {
    public RegistreringResponseDto data;
    public boolean journalfort;
}
