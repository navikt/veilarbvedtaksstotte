package no.nav.veilarbvedtaksstotte.client.veilederogenhet.dto;

import lombok.Value;
import org.jspecify.annotations.Nullable;

@Value
public class PortefoljeEnhet {
    String enhetId;
    String navn;

    public @Nullable String getEnhetId() {
        return enhetId;
    }
}
