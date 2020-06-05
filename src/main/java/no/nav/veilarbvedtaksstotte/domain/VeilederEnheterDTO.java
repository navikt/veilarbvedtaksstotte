package no.nav.veilarbvedtaksstotte.domain;

import lombok.Value;

import java.util.List;

@Value
public class VeilederEnheterDTO {
    String ident;
    List<PortefoljeEnhet> enhetliste;
}
