package no.nav.fo.veilarbvedtaksstotte.domain;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.KildeType;

@Data
@Accessors(chain = true)
public class Opplysning {
    long id;
    long vedtak_id;
    KildeType kilde;
    String json; //BLOB
}
