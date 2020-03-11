package no.nav.veilarbvedtaksstotte.config;

import no.nav.veilarbvedtaksstotte.client.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
        DokumentClient.class,
        SAFClient.class,
        ArenaClient.class,
        SAFClient.class,
        CVClient.class,
        RegistreringClient.class,
        EgenvurderingClient.class,
        VeiledereOgEnhetClient.class,
        OppfolgingClient.class,
		OppgaveClient.class
})

public class ClientConfig {}
