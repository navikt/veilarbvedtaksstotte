package no.nav.veilarbvedtaksstotte.config;

import no.nav.common.utils.EnvironmentUtils;
import no.nav.veilarbvedtaksstotte.client.arena.ArenaClient;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.SafClient;
import no.nav.veilarbvedtaksstotte.client.dokument.DokumentClient;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.SafClientImpl;
import no.nav.veilarbvedtaksstotte.client.dokument.DokumentClientImpl;
import no.nav.veilarbvedtaksstotte.client.egenvurdering.EgenvurderingClient;
import no.nav.veilarbvedtaksstotte.client.egenvurdering.EgenvurderingClientImpl;
import no.nav.veilarbvedtaksstotte.client.oppfolging.OppfolgingClient;
import no.nav.veilarbvedtaksstotte.client.person.VeilarbpersonClient;
import no.nav.veilarbvedtaksstotte.client.person.VeilarbpersonClientImpl;
import no.nav.veilarbvedtaksstotte.client.registrering.RegistreringClient;
import no.nav.veilarbvedtaksstotte.client.registrering.RegistreringClientImpl;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.VeiledereOgEnhetClient;
import no.nav.veilarbvedtaksstotte.client.arena.ArenaClientImpl;
import no.nav.veilarbvedtaksstotte.client.oppfolging.OppfolgingClientImpl;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.VeiledereOgEnhetClientImpl;
import no.nav.veilarbvedtaksstotte.service.AuthService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static no.nav.common.utils.UrlUtils.createNaisAdeoIngressUrl;
import static no.nav.common.utils.UrlUtils.createNaisPreprodIngressUrl;

@Configuration
public class ClientConfig {

    @Bean
    public ArenaClient arenaClient() {
        return new ArenaClientImpl(naisPreprodOrNaisAdeoIngress("veilarbarena",  true));
    }

    @Bean
    public DokumentClient dokumentClient() {
        return new DokumentClientImpl(naisPreprodOrNaisAdeoIngress("veilarbdokument", true));
    }

    @Bean
    public EgenvurderingClient egenvurderingClient() {
        return new EgenvurderingClientImpl(naisPreprodOrNaisAdeoIngress("veilarbvedtakinfo", true));
    }

    @Bean
    public OppfolgingClient oppfolgingClient() {
        return new OppfolgingClientImpl(naisPreprodOrNaisAdeoIngress("veilarboppfolging", true));
    }

    @Bean
    public VeilarbpersonClient personClient(AuthService authService) {
        return new VeilarbpersonClientImpl(naisPreprodOrNaisAdeoIngress("veilarbperson", true), authService::getInnloggetBrukerToken);
    }

    @Bean
    public RegistreringClient registreringClient() {
        return new RegistreringClientImpl(naisPreprodOrNaisAdeoIngress("veilarbregistrering", true));
    }

    @Bean
    public SafClient safClient() {
        return new SafClientImpl(naisPreprodOrNaisAdeoIngress("saf", false));
    }

    @Bean
    public VeiledereOgEnhetClient veilederOgEnhetClient() {
        return new VeiledereOgEnhetClientImpl(naisPreprodOrNaisAdeoIngress("veilarbveileder", true));
    }

    private static String naisPreprodOrNaisAdeoIngress(String appName, boolean withAppContextPath) {
        return EnvironmentUtils.isDevelopment().orElse(false)
                ? createNaisPreprodIngressUrl(appName, "q1", withAppContextPath)
                : createNaisAdeoIngressUrl(appName, withAppContextPath);
    }

}
