package no.nav.veilarbvedtaksstotte.client.dokument;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class ProduserDokumentV2DTO {
    String brukerFnr;
    MalType malType;
    String enhetId;
    String begrunnelse;
    List<String> opplysninger;
    boolean utkast;
}
