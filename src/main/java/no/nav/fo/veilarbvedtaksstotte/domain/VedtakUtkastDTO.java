package no.nav.fo.veilarbvedtaksstotte.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.Hovedmal;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.InnsatsGruppe;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@ToString
@EqualsAndHashCode
public class VedtakUtkastDTO {
    Hovedmal hovedmal;
    InnsatsGruppe innsatsGruppe;
    LocalDateTime sistOppdatert;
    String begrunnelse;
    Veileder veileder;
}
