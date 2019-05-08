package no.nav.fo.veilarbvedtaksstotte.domain;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.Hovedmal;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.Innsatsgruppe;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.OpplysningsType;

import java.util.List;
import java.util.stream.Collectors;

@Data
@Accessors(chain = true)
public class VedtakDTO {
    Hovedmal hovedmal;
    Innsatsgruppe innsatsgruppe;
    String begrunnelse;
    //TODO: skal disse to være med her?
    //List<OpplysningsType> opplysninger;
    //List<String> andreOpplysninger;

    public Vedtak tilVedtak() {
        return new Vedtak()
                .setHovedmal(hovedmal)
                .setInnsatsgruppe(innsatsgruppe)
                .setBegrunnelse(begrunnelse);
                //.setOpplysningsTyper(opplysninger)
                //.setAndreOpplysninger(); TODO: må fikse noe her
    }

}
