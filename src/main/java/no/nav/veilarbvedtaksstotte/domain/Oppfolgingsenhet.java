package no.nav.veilarbvedtaksstotte.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Value;

@Value
public class Oppfolgingsenhet {
    @JsonAlias("nav_kontor")
    String navKontor;
}
