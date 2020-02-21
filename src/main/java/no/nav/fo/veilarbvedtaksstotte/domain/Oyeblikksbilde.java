package no.nav.fo.veilarbvedtaksstotte.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.OyeblikksbildeType;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class Oyeblikksbilde {
    long vedtakId;
    OyeblikksbildeType oyeblikksbildeType;
    String json;
}
