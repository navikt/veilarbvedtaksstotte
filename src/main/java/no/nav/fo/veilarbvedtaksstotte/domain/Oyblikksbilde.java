package no.nav.fo.veilarbvedtaksstotte.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.OyblikksbildeType;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class Oyblikksbilde {
    long vedtakId;
    OyblikksbildeType oyblikksbildeType;
    String json;
}
