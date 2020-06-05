package no.nav.veilarbvedtaksstotte.domain.dialog;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class Melding {
    long id;
    long vedtakId;
    LocalDateTime opprettet;
}
