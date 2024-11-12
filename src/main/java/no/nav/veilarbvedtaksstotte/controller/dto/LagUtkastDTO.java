package no.nav.veilarbvedtaksstotte.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import no.nav.common.types.identer.Fnr;

@Data
public class LagUtkastDTO {

    @Schema(description = "Fødselsnummeret til brukeren vedtaksutkastet skal knyttes til")
    Fnr fnr;
}
