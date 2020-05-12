package no.nav.veilarbvedtaksstotte.domain;

import com.google.gson.annotations.SerializedName;
import lombok.Value;

@Value
public class Oppfolgingsenhet {
    @SerializedName("nav_kontor")
    String navKontor;
}
