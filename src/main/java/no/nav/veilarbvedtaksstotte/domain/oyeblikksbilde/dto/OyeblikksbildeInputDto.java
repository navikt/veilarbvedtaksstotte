package no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.OyeblikksbildeType;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class OyeblikksbildeInputDto {
    public long vedtakId;
    public OyeblikksbildeType oyeblikksbildeType;
    public String json;
}
