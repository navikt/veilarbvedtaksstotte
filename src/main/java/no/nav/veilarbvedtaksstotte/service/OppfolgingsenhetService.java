package no.nav.veilarbvedtaksstotte.service;

import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.VeilarboppfolgingClient;
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.dto.OppfolgingsenhetDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class OppfolgingsenhetService {
    private static final Logger log = LoggerFactory.getLogger(OppfolgingsenhetService.class);
    private final VeilarboppfolgingClient veilarboppfolgingClient;

    public OppfolgingsenhetService(VeilarboppfolgingClient veilarboppfolgingClient) {
        this.veilarboppfolgingClient = veilarboppfolgingClient;
    }

    public Optional<EnhetId> hentOppfolgingsenhet(Fnr fnr) {
        try {
            Optional<EnhetId> enhet = veilarboppfolgingClient.hentOppfolgingsenhet(fnr)
                    .map(OppfolgingsenhetDTO::getEnhetId)
                    .filter(enhetId -> enhetId != null && !enhetId.isBlank())
                    .map(EnhetId::of);
            if (enhet.isEmpty()) {
                log.warn("hentOppfolgingsenhet returnerte tom for fnr (maskert). Kontroller GraphQL-kall mot veilarboppfolging.");
            }
            return enhet;
        } catch (Exception e) {
            log.error("Feil ved henting av oppfølgingsenhet fra veilarboppfolging: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }
}
