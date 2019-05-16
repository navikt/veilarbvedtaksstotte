package no.nav.fo.veilarbvedtaksstotte.domain;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.OpplysningsType;

@Data
@Accessors(chain = true)
public class Opplysning {
    long vedtakId;
    boolean valgt;
    OpplysningsType opplysningsType;
    String json;
}
