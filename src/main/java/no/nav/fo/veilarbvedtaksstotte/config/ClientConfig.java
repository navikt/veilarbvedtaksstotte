package no.nav.fo.veilarbvedtaksstotte.config;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbvedtaksstotte.client.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Slf4j
@Configuration
@Import({
        DokumentClient.class,
        PersonClient.class,
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
