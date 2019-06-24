package no.nav.fo.veilarbvedtaksstotte.domain;

import lombok.Value;
import no.nav.apiapp.security.veilarbabac.Bruker;

@Value
public class AuthKontekst {
    Bruker bruker;
    String oppfolgingsenhet;
}
