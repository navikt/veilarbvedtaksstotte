package no.nav.veilarbvedtaksstotte.client.dokument;

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

    public String getBrukerFnr() {
        return this.brukerFnr;
    }

    public MalType getMalType() {
        return this.malType;
    }

    public String getEnhetId() {
        return this.enhetId;
    }

    public String getBegrunnelse() {
        return this.begrunnelse;
    }

    public List<String> getOpplysninger() {
        return this.opplysninger;
    }
}
