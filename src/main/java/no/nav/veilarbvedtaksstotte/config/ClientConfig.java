package no.nav.veilarbvedtaksstotte.config;

import no.nav.common.sts.SystemUserTokenProvider;
import no.nav.common.utils.EnvironmentUtils;
import no.nav.veilarbvedtaksstotte.client.arena.VeilarbarenaClient;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.DokarkivClient;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.DokarkivClientImpl;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.SafClient;
import no.nav.veilarbvedtaksstotte.client.dokdistfordeling.DokdistribusjonClient;
import no.nav.veilarbvedtaksstotte.client.dokdistfordeling.DokdistribusjonClientImpl;
import no.nav.veilarbvedtaksstotte.client.dokument.VeilarbdokumentClient;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.SafClientImpl;
import no.nav.veilarbvedtaksstotte.client.dokument.VeilarbdokumentClientImpl;
import no.nav.veilarbvedtaksstotte.client.egenvurdering.VeilarbvedtakinfoClient;
import no.nav.veilarbvedtaksstotte.client.egenvurdering.VeilarbvedtakinfoClientImpl;
import no.nav.veilarbvedtaksstotte.client.oppfolging.VeilarboppfolgingClient;
import no.nav.veilarbvedtaksstotte.client.person.VeilarbpersonClient;
import no.nav.veilarbvedtaksstotte.client.person.VeilarbpersonClientImpl;
import no.nav.veilarbvedtaksstotte.client.registrering.VeilarbregistreringClient;
import no.nav.veilarbvedtaksstotte.client.registrering.VeilarbregistreringClientImpl;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.VeilarbveilederClient;
import no.nav.veilarbvedtaksstotte.client.arena.VeilarbarenaClientImpl;
import no.nav.veilarbvedtaksstotte.client.oppfolging.VeilarboppfolgingClientImpl;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.VeilarbveilederClientImpl;
import no.nav.veilarbvedtaksstotte.service.AuthService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static no.nav.common.utils.UrlUtils.createNaisAdeoIngressUrl;
import static no.nav.common.utils.UrlUtils.createNaisPreprodIngressUrl;

@Configuration
public class ClientConfig {

    @Bean
    public VeilarbarenaClient arenaClient() {
        return new VeilarbarenaClientImpl(naisPreprodOrNaisAdeoIngress("veilarbarena",  true));
    }

    @Bean
    public VeilarbdokumentClient dokumentClient() {
        return new VeilarbdokumentClientImpl(naisPreprodOrNaisAdeoIngress("veilarbdokument", true));
    }

    @Bean
    public VeilarbvedtakinfoClient egenvurderingClient() {
        return new VeilarbvedtakinfoClientImpl(naisPreprodOrNaisAdeoIngress("veilarbvedtakinfo", true));
    }

    @Bean
    public VeilarboppfolgingClient oppfolgingClient() {
        return new VeilarboppfolgingClientImpl(naisPreprodOrNaisAdeoIngress("veilarboppfolging", true));
    }

    @Bean
    public VeilarbpersonClient personClient(AuthService authService) {
        return new VeilarbpersonClientImpl(naisPreprodOrNaisAdeoIngress("veilarbperson", true), authService::getInnloggetBrukerToken);
    }

    @Bean
    public VeilarbregistreringClient registreringClient() {
        return new VeilarbregistreringClientImpl(naisPreprodOrNaisAdeoIngress("veilarbregistrering", true));
    }

    @Bean
    public SafClient safClient() {
        return new SafClientImpl(naisPreprodOrNaisAdeoIngress("saf", false));
    }

    @Bean
    public VeilarbveilederClient veilederOgEnhetClient() {
        return new VeilarbveilederClientImpl(naisPreprodOrNaisAdeoIngress("veilarbveileder", true));
    }

    @Bean
    public DokarkivClient dokarkivClient(SystemUserTokenProvider systemUserTokenProvider) {
        return new DokarkivClientImpl(naisPreprodOrNaisAdeoIngress("dokarkiv", false), systemUserTokenProvider);
    }

    @Bean
    public DokdistribusjonClient dokDistribusjonClient() {
        return new DokdistribusjonClientImpl(naisPreprodOrNaisAdeoIngress("dokdistfordeling", false));
    }

    private static String naisPreprodOrNaisAdeoIngress(String appName, boolean withAppContextPath) {
        return EnvironmentUtils.isDevelopment().orElse(false)
                ? createNaisPreprodIngressUrl(appName, "q1", withAppContextPath)
                : createNaisAdeoIngressUrl(appName, withAppContextPath);
    }

}
