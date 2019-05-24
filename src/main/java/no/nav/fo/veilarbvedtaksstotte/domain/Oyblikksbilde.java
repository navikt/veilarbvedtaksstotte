package no.nav.fo.veilarbvedtaksstotte.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.KildeType;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class Oyblikksbilde {
    long vedtakId;
    KildeType kildeType;
    String json;
}
