package no.nav.fo.veilarbvedtaksstotte.domain;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.MalType;

import java.util.List;

@Data
@Accessors(chain = true)
public class SendDokumentDTO {
    DokumentPerson bruker;
    DokumentPerson mottaker;
    MalType malType;
    String veilederEnhet;
    String begrunnelse;
    List<String> kilder;
}
