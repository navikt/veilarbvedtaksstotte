package no.nav.fo.veilarbvedtaksstotte.domain;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class AndreOpplysninger {
    long id;
    long vedtakId;
    String tekst;
}
