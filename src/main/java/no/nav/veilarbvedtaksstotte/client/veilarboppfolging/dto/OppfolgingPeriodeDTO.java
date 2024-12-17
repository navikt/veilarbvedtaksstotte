package no.nav.veilarbvedtaksstotte.client.veilarboppfolging.dto;

import lombok.Data;

import java.time.ZonedDateTime;
import java.util.UUID;

@Data
public class OppfolgingPeriodeDTO {
    public UUID uuid;
    public ZonedDateTime startDato;
    public ZonedDateTime sluttDato;

    @java.beans.ConstructorProperties({"uuid","startDato", "sluttDato"})
    public OppfolgingPeriodeDTO(UUID uuid, ZonedDateTime startDato, ZonedDateTime sluttDato) {
        this.uuid = uuid;
        this.startDato = startDato;
        this.sluttDato = sluttDato;
    }

    public OppfolgingPeriodeDTO() {
    }
}
