package no.nav.veilarbvedtaksstotte.config;

import no.nav.veilarbvedtaksstotte.client.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static no.nav.common.utils.UrlUtils.clusterUrlForApplication;

@Configuration
@Profile("!local")
public class ClientConfig {

    @Bean
    public ArenaClient arenaClient() {
        return new ArenaClient(clusterUrlForApplication("veilarbarena", true));
    }

    @Bean
    public DokumentClient dokumentClient() {
        return new DokumentClient(clusterUrlForApplication("veilarbdokument", true));
    }

    @Bean
    public EgenvurderingClient egenvurderingClient() {
        return new EgenvurderingClient(clusterUrlForApplication("veilarbvedtakinfo", true));
    }

    @Bean
    public OppfolgingClient oppfolgingClient() {
        return new OppfolgingClient(clusterUrlForApplication("veilarboppfolging", true));
    }

    @Bean
    public PamCvClient pamCvClient() {
        // PAM CV finnes ikke i Q1, gå mot Q0 istedenfor
        String pamCvUrl = clusterUrlForApplication("pam-cv-api").replace("q1", "q0");
        return new PamCvClient(pamCvUrl);
    }

    @Bean
    public PersonClient personClient() {
        return new PersonClient(clusterUrlForApplication("veilarbperson", true));
    }

    @Bean
    public RegistreringClient registreringClient() {
        return new RegistreringClient(clusterUrlForApplication("veilarbregistrering", true));
    }

    @Bean
    public SafClient safClient() {
        return new SafClient(clusterUrlForApplication("saf", false));
    }

    @Bean
    public VeiledereOgEnhetClient veilederOgEnhetClient() {
        return new VeiledereOgEnhetClient(clusterUrlForApplication("veilarbveileder", true));
    }

}
