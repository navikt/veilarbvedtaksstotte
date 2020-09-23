package no.nav.veilarbvedtaksstotte.config;

import no.nav.veilarbvedtaksstotte.client.*;
import no.nav.veilarbvedtaksstotte.client.api.*;
import no.nav.veilarbvedtaksstotte.service.AuthService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static no.nav.common.utils.UrlUtils.clusterUrlForApplication;

@Configuration
public class ClientConfig {

    @Bean
    public ArenaClient arenaClient() {
        return new ArenaClientImpl(clusterUrlForApplication("veilarbarena", true));
    }

    @Bean
    public DokumentClient dokumentClient() {
        return new DokumentClientImpl(clusterUrlForApplication("veilarbdokument", true));
    }

    @Bean
    public EgenvurderingClient egenvurderingClient() {
        return new EgenvurderingClientImpl(clusterUrlForApplication("veilarbvedtakinfo", true));
    }

    @Bean
    public OppfolgingClient oppfolgingClient() {
        return new OppfolgingClientImpl(clusterUrlForApplication("veilarboppfolging", true));
    }

    @Bean
    public VeilarbpersonClient personClient(AuthService authService) {
        return new VeilarbpersonClientImpl(clusterUrlForApplication("veilarbperson", true), authService::getInnloggetBrukerToken);
    }

    @Bean
    public RegistreringClient registreringClient() {
        return new RegistreringClientImpl(clusterUrlForApplication("veilarbregistrering", true));
    }

    @Bean
    public SafClient safClient() {
        return new SafClientImpl(clusterUrlForApplication("saf", false));
    }

    @Bean
    public VeiledereOgEnhetClient veilederOgEnhetClient() {
        return new VeiledereOgEnhetClientImpl(clusterUrlForApplication("veilarbveileder", true));
    }

}
