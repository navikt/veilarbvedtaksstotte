package no.nav.fo.veilarbvedtaksstotte.domain;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.Hovedmal;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.Innsatsgruppe;

import java.util.List;
import java.util.stream.Collectors;

@Data
@Accessors(chain = true)
public class VedtakDTO {
    Hovedmal hovedmal;
    Innsatsgruppe innsatsgruppe;
    String begrunnelse;
    List<String> opplysninger;

    public Vedtak tilVedtakFraUtkast() {
        List<Opplysning> opplysninger = this.opplysninger.stream()
                .map(opplysning -> new Opplysning().setTekst(opplysning))
                .collect(Collectors.toList());

        return new Vedtak()
                .setHovedmal(hovedmal)
                .setInnsatsgruppe(innsatsgruppe)
                .setBegrunnelse(begrunnelse)
                .setOpplysninger(opplysninger);
    }
}
