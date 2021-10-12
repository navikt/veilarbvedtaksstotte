package no.nav.veilarbvedtaksstotte.controller.dto;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.veilarbvedtaksstotte.domain.vedtak.HovedmalDetaljert;
import no.nav.veilarbvedtaksstotte.domain.vedtak.InnsatsgruppeDetaljert;

@Data
@Accessors(chain = true)
public class KodeverkDTO {
    InnsatsgruppeDetaljert[] innsatsgrupper;
    HovedmalDetaljert[] hovedmal;

    public KodeverkDTO() {
        this.innsatsgrupper = InnsatsgruppeDetaljert.values();
        this.hovedmal = HovedmalDetaljert.values();
    }
}
