package no.nav.fo.veilarbvedtaksstotte.domain;

import lombok.Value;

@Value
public class AuthKontekst {
    String fnr;
    String aktorId;
    String oppfolgingsenhet;
}
