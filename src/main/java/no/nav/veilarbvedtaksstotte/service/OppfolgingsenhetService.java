package no.nav.veilarbvedtaksstotte.service;

import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.VeilarboppfolgingClient;
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.dto.OppfolgingsenhetDTO;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class OppfolgingsenhetService {
    private final VeilarboppfolgingClient veilarboppfolgingClient;

    public OppfolgingsenhetService(VeilarboppfolgingClient veilarboppfolgingClient) {
        this.veilarboppfolgingClient = veilarboppfolgingClient;
    }

    public Optional<EnhetId> hentOppfolgingsenhet(Fnr fnr) {
        return veilarboppfolgingClient.hentOppfolgingsenhet(fnr)
                .map(OppfolgingsenhetDTO::getEnhetId)
                .filter(enhetId -> enhetId != null && !enhetId.isBlank())
                .map(EnhetId::of);
    }
}
