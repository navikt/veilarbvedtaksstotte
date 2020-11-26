package no.nav.veilarbvedtaksstotte.client.veilederogenhet;

import lombok.Value;

import java.util.List;

@Value
public class VeilederEnheterDTO {
    String ident;
    List<PortefoljeEnhet> enhetliste;
}
