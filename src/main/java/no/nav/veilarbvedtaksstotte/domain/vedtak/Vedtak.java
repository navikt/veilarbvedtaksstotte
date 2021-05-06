package no.nav.veilarbvedtaksstotte.domain.vedtak;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;

@SuperBuilder
@Getter
@Setter
@EqualsAndHashCode
public abstract class Vedtak {
    long id;
    String aktorId;
    Hovedmal hovedmal;
    Innsatsgruppe innsatsgruppe;
    String begrunnelse;
    String veilederIdent;
    String veilederNavn;
    String oppfolgingsenhetId;
    String oppfolgingsenhetNavn;
    String beslutterIdent;
    String beslutterNavn;
    List<String> opplysninger;
}
