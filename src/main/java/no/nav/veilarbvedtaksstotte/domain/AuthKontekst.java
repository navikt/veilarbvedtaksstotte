package no.nav.veilarbvedtaksstotte.domain;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class AuthKontekst {
    String fnr;
    String aktorId;
    String oppfolgingsenhet;
}
