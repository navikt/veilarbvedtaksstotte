package no.nav.fo.veilarbvedtaksstotte.domain;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.Hovedmal;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.Innsatsgruppe;

import java.util.List;

@Data
@Accessors(chain = true)
public class VedtakDTO {
    Hovedmal hovedmal;
    Innsatsgruppe innsatsgruppe;
    String begrunnelse;
    List<String> opplysninger;
}
