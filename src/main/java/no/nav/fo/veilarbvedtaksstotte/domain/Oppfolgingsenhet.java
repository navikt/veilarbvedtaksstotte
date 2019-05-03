package no.nav.fo.veilarbvedtaksstotte.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class Oppfolgingsenhet {
    @JsonProperty("nav_kontor")
    String navKontor;
}
