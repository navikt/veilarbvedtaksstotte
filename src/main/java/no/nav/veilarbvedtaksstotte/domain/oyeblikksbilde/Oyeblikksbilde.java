package no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class Oyeblikksbilde {
    long vedtakId;
    OyeblikksbildeType oyeblikksbildeType;
    String json;
}
