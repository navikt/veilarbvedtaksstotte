package no.nav.veilarbvedtaksstotte.client.arena.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Value;

@Value
public class VeilarbArenaOppfolging {
    @JsonAlias("nav_kontor")
    String navKontor;
    String formidlingsgruppekode;
    String kvalifiseringsgruppekode;
}
