package no.nav.fo.veilarbvedtaksstotte.domain;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.Hovedmal;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.Innsatsgruppe;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.VedtakStatus;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class Vedtak {
    long id;
    Hovedmal hovedmal;
    Innsatsgruppe innsatsgruppe;
    VedtakStatus vedtakStatus;
    LocalDateTime sistOppdatert;
    String begrunnelse;
    Veileder veileder;
    boolean gjeldende;
}
