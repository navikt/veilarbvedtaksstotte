package no.nav.veilarbvedtaksstotte.config;

import no.nav.veilarbvedtaksstotte.client.*;
import no.nav.veilarbvedtaksstotte.client.api.*;
import no.nav.veilarbvedtaksstotte.service.AuthService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static no.nav.common.utils.UrlUtils.createServiceUrl;

@Configuration
public class ClientConfig {

    @Bean
    public ArenaClient arenaClient() {
        return new ArenaClientImpl(createServiceUrl("veilarbarena", true));
    }

    @Bean
    public DokumentClient dokumentClient() {
        return new DokumentClientImpl(createServiceUrl("veilarbdokument", true));
    }

    @Bean
    public EgenvurderingClient egenvurderingClient() {
        return new EgenvurderingClientImpl(createServiceUrl("veilarbvedtakinfo", true));
    }

    @Bean
    public OppfolgingClient oppfolgingClient() {
        return new OppfolgingClientImpl(createServiceUrl("veilarboppfolging", true));
    }

    @Bean
    public VeilarbpersonClient personClient(AuthService authService) {
        return new VeilarbpersonClientImpl(createServiceUrl("veilarbperson", true), authService::getInnloggetBrukerToken);
    }

    @Bean
    public RegistreringClient registreringClient() {
        return new RegistreringClientImpl(createServiceUrl("veilarbregistrering", true));
    }

    @Bean
    public SafClient safClient() {
        return new SafClientImpl(createServiceUrl("saf", false));
    }

    @Bean
    public VeiledereOgEnhetClient veilederOgEnhetClient() {
        return new VeiledereOgEnhetClientImpl(createServiceUrl("veilarbveileder", true));
    }

}
