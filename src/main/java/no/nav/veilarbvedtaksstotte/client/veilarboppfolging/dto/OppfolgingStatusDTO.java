package no.nav.veilarbvedtaksstotte.client.veilarboppfolging.dto;

import lombok.Data;

@Data
public class OppfolgingStatusDTO {
    public boolean erUnderOppfolging;

    @java.beans.ConstructorProperties({"erUnderOppfolging"})
    public OppfolgingStatusDTO(boolean erUnderOppfolging) {
        this.erUnderOppfolging = erUnderOppfolging;
    }

    public OppfolgingStatusDTO() {
    }
}
