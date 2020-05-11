package no.nav.veilarbvedtaksstotte.config;

import no.nav.veilarbvedtaksstotte.client.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.mockito.Mockito.mock;

@Configuration
public class ClientConfig {

    // TODO: Mock kall til clients eller bruk interfaces

    @Bean
    public ArenaClient arenaClient() {
        return mock(ArenaClient.class);
    }

    @Bean
    public DokumentClient dokumentClient() {
        return mock(DokumentClient.class);
    }

    @Bean
    public EgenvurderingClient egenvurderingClient() {
        return mock(EgenvurderingClient.class);
    }

    @Bean
    public OppfolgingClient oppfolgingClient() {
        return mock(OppfolgingClient.class);
    }

    @Bean
    public PamCvClient pamCvClient() {
        return mock(PamCvClient.class);
    }

    @Bean
    public PersonClient personClient() {
        return mock(PersonClient.class);
    }

    @Bean
    public RegistreringClient registreringClient() {
        return mock(RegistreringClient.class);
    }

    @Bean
    public SafClient safClient() {
        return mock(SafClient.class);
    }

    @Bean
    public VeiledereOgEnhetClient veilederOgEnhetClient() {
        return mock(VeiledereOgEnhetClient.class);
    }

}
