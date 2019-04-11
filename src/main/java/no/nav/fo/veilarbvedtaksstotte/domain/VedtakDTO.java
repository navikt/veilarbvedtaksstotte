package no.nav.fo.veilarbvedtaksstotte.domain;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.Hovedmal;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.Innsatsgruppe;

@Data
@Accessors(chain = true)
public class VedtakDTO {
    Hovedmal hovedmal;
    Innsatsgruppe innsatsgruppe;
    String begrunnelse;

    public Vedtak tilVedtak() {
        return new Vedtak()
                .setHovedmal(hovedmal)
                .setInnsatsgruppe(innsatsgruppe)
                .setBegrunnelse(begrunnelse);
    }

}
