package no.nav.veilarbvedtaksstotte.controller.dto;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Hovedmal;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsgruppe;

import java.util.List;

@Data
@Accessors(chain = true)
public class OppdaterUtkastDTO {
    Hovedmal hovedmal;
    Innsatsgruppe innsatsgruppe;
    String begrunnelse;
    List<String> opplysninger;
}
