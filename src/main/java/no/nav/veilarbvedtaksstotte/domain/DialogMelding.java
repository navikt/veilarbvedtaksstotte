package no.nav.veilarbvedtaksstotte.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class DialogMelding {
    long id;
    long vedtakId;
    String melding;
    LocalDateTime opprettet;
    String opprettetAvIdent;
}
