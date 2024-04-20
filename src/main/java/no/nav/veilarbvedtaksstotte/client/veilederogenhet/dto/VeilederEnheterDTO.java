package no.nav.veilarbvedtaksstotte.client.veilederogenhet.dto;

import lombok.Value;

import java.util.List;

@Value
public class VeilederEnheterDTO {
    String ident;
    List<PortefoljeEnhet> enhetliste;
}
