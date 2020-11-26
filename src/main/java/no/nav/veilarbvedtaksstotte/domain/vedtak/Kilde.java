package no.nav.veilarbvedtaksstotte.domain.vedtak;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Kilde {
    long vedtakId;
    String tekst;
}
