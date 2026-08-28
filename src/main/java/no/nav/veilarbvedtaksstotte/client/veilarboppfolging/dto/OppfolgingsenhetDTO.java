package no.nav.veilarbvedtaksstotte.client.veilarboppfolging.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OppfolgingsenhetDTO {
    private String enhetId;
    private String navn;

    public OppfolgingsenhetDTO() {}
}
