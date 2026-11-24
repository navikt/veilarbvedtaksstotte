package no.nav.veilarbvedtaksstotte.client.veilederogenhet.dto;

import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.util.List;

@Value
public class VeilederEnheterDTO {
    String ident;
    List<PortefoljeEnhet> enhetliste;

    public @Nullable List<@Nullable PortefoljeEnhet> getEnhetliste() {
        return enhetliste;
    }
}
