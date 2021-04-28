package no.nav.veilarbvedtaksstotte.client.veilarboppfolging;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OppfolgingPeriodeDTO {
    public LocalDateTime startDato;
    public LocalDateTime sluttDato;

    @java.beans.ConstructorProperties({"startDato", "sluttDato"})
    public OppfolgingPeriodeDTO(LocalDateTime startDato, LocalDateTime sluttDato) {
        this.startDato = startDato;
        this.sluttDato = sluttDato;
    }

    public OppfolgingPeriodeDTO() {
    }
}
