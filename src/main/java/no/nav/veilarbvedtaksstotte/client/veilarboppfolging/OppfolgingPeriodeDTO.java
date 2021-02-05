package no.nav.veilarbvedtaksstotte.client.veilarboppfolging;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OppfolgingPeriodeDTO {
    public LocalDateTime startDato;
    public LocalDateTime sluttDato;
}
