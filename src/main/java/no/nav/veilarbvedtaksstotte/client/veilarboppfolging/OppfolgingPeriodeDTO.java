package no.nav.veilarbvedtaksstotte.client.veilarboppfolging;

import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class OppfolgingPeriodeDTO {
    public ZonedDateTime startDato;
    public ZonedDateTime sluttDato;

    @java.beans.ConstructorProperties({"startDato", "sluttDato"})
    public OppfolgingPeriodeDTO(ZonedDateTime startDato, ZonedDateTime sluttDato) {
        this.startDato = startDato;
        this.sluttDato = sluttDato;
    }

    public OppfolgingPeriodeDTO() {
    }
}
