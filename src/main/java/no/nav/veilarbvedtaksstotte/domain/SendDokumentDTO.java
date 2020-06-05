package no.nav.veilarbvedtaksstotte.domain;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.veilarbvedtaksstotte.domain.enums.MalType;

import java.util.List;

@Data
@Accessors(chain = true)
public class SendDokumentDTO {
    String brukerFnr;
    MalType malType;
    String enhetId;
    String begrunnelse;
    List<String> opplysninger;
}
