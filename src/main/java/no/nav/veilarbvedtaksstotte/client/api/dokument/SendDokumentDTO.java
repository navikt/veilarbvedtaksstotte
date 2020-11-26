package no.nav.veilarbvedtaksstotte.client.api.dokument;

import lombok.Data;
import lombok.experimental.Accessors;

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
