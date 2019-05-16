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
    List<OpplysningsType> opplysninger;
    List<String> andreOpplysninger;

    public Vedtak tilVedtakFraUtkast() {
        List<Opplysning> opplysninger = this.opplysninger.stream()
                // setter valgt fordi vi kun oppdaterer de opplysningene som er valgt, men til slutt lagrer alle kilder
                .map(opplysning -> new Opplysning().setOpplysningsType(opplysning).setValgt(true))
                .collect(Collectors.toList());
        List<AnnenOpplysning> annenOpplysning = this.andreOpplysninger.stream()
                .map(opplysning -> new AnnenOpplysning().setTekst(opplysning))
                .collect(Collectors.toList());

        return new Vedtak()
                .setHovedmal(hovedmal)
                .setInnsatsgruppe(innsatsgruppe)
                .setBegrunnelse(begrunnelse)
                .setOpplysninger(opplysninger)
                .setAnnenOpplysning(annenOpplysning);
    }
}
