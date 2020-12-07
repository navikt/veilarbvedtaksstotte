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

    public ProduserDokumentV2DTO setBrukerFnr(String brukerFnr) {
        this.brukerFnr = brukerFnr;
        return this;
    }

    public ProduserDokumentV2DTO setMalType(MalType malType) {
        this.malType = malType;
        return this;
    }

    public ProduserDokumentV2DTO setEnhetId(String enhetId) {
        this.enhetId = enhetId;
        return this;
    }

    public ProduserDokumentV2DTO setBegrunnelse(String begrunnelse) {
        this.begrunnelse = begrunnelse;
        return this;
    }

    public ProduserDokumentV2DTO setOpplysninger(List<String> opplysninger) {
        this.opplysninger = opplysninger;
        return this;
    }

    public ProduserDokumentV2DTO setUtkast(boolean utkast) {
        this.utkast = utkast;
        return this;
    }
}
