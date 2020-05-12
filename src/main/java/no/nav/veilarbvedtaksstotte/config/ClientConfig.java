package no.nav.veilarbvedtaksstotte.config;

import no.nav.veilarbvedtaksstotte.client.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static no.nav.veilarbvedtaksstotte.utils.UrlUtils.lagClusterUrl;

@Configuration
@Profile("!local")
public class ClientConfig {

    @Bean
    public ArenaClient arenaClient() {
        return new ArenaClient(lagClusterUrl("veilarbarena"));
    }

    @Bean
    public DokumentClient dokumentClient() {
        return new DokumentClient(lagClusterUrl("veilarbdokument"));
    }

    @Bean
    public EgenvurderingClient egenvurderingClient() {
        return new EgenvurderingClient(lagClusterUrl("veilarbvedtakinfo"));
    }

    @Bean
    public OppfolgingClient oppfolgingClient() {
        return new OppfolgingClient(lagClusterUrl("veilarboppfolging"));
    }

    @Bean
    public PamCvClient pamCvClient() {
        // PAM CV finnes ikke i Q1, gå mot Q0 istedenfor
        String pamCvUrl = lagClusterUrl("pam-cv-api").replace("q1", "q0");
        return new PamCvClient(lagClusterUrl(pamCvUrl));
    }

    @Bean
    public PersonClient personClient() {
        return new PersonClient(lagClusterUrl("veilarbperson"));
    }

    @Bean
    public RegistreringClient registreringClient() {
        return new RegistreringClient(lagClusterUrl("veilarbregistrering"));
    }

    @Bean
    public SafClient safClient() {
        return new SafClient(lagClusterUrl("saf", false));
    }

    @Bean
    public VeiledereOgEnhetClient veilederOgEnhetClient() {
        return new VeiledereOgEnhetClient(lagClusterUrl("veilarbveileder"));
    }

}
