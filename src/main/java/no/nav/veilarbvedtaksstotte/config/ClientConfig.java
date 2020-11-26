package no.nav.veilarbvedtaksstotte.config;

import no.nav.common.utils.EnvironmentUtils;
import no.nav.veilarbvedtaksstotte.client.api.*;
import no.nav.veilarbvedtaksstotte.client.api.dokarkiv.SafClient;
import no.nav.veilarbvedtaksstotte.client.api.dokument.DokumentClient;
import no.nav.veilarbvedtaksstotte.client.impl.*;
import no.nav.veilarbvedtaksstotte.client.api.oppfolging.OppfolgingClient;
import no.nav.veilarbvedtaksstotte.client.api.person.VeilarbpersonClient;
import no.nav.veilarbvedtaksstotte.client.api.registrering.RegistreringClient;
import no.nav.veilarbvedtaksstotte.client.api.veilederogenhet.VeiledereOgEnhetClient;
import no.nav.veilarbvedtaksstotte.client.impl.arena.ArenaClientImpl;
import no.nav.veilarbvedtaksstotte.client.impl.oppfolging.OppfolgingClientImpl;
import no.nav.veilarbvedtaksstotte.client.impl.veilederogenhet.VeiledereOgEnhetClientImpl;
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
