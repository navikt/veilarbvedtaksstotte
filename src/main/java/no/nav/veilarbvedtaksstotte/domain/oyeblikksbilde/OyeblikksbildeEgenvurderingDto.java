package no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class OyeblikksbildeEgenvurderingDto {
    public EgenvurderingDto data;
    public boolean journalfort;
}
