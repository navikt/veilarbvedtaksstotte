package no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import no.nav.veilarbvedtaksstotte.client.person.dto.CvInnhold;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class OyeblikksbildeCvDto {
    public CvInnhold data;
    public boolean journalfort;
}
