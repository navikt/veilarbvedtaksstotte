package no.nav.veilarbvedtaksstotte.client.api.veilederogenhet;

import lombok.Value;

import java.util.List;

@Value
public class VeilederEnheterDTO {
    String ident;
    List<PortefoljeEnhet> enhetliste;
}
