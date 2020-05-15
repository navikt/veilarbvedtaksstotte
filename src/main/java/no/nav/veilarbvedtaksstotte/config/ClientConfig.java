package no.nav.veilarbvedtaksstotte.config;

import no.nav.veilarbvedtaksstotte.client.*;
import no.nav.veilarbvedtaksstotte.client.api.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static no.nav.common.utils.UrlUtils.clusterUrlForApplication;

@Configuration
@Profile("!local")
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
    public PamCvClient pamCvClient() {
        // PAM CV finnes ikke i Q1, gå mot Q0 istedenfor
        String pamCvUrl = clusterUrlForApplication("pam-cv-api", true).replace("q1", "q0");
        return new PamCvClientImpl(pamCvUrl);
    }

    @Bean
    public PersonClient personClient() {
        return new PersonClientImpl(clusterUrlForApplication("veilarbperson", true));
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
