package no.nav.veilarbvedtaksstotte.config;

import no.nav.common.abac.AbacCachedClient;
import no.nav.common.abac.AbacClient;
import no.nav.common.abac.AbacHttpClient;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.client.aktoroppslag.*;
import no.nav.common.client.pdl.PdlClient;
import no.nav.common.client.pdl.PdlClientImpl;
import no.nav.common.client.norg2.CachedNorg2Client;
import no.nav.common.client.norg2.Norg2Client;
import no.nav.common.client.norg2.NorgHttp2Client;
import no.nav.common.featuretoggle.UnleashClient;
import no.nav.common.featuretoggle.UnleashClientImpl;
import no.nav.common.job.leader_election.LeaderElectionClient;
import no.nav.common.job.leader_election.LeaderElectionHttpClient;
import no.nav.common.metrics.InfluxClient;
import no.nav.common.metrics.MetricsClient;
import no.nav.common.sts.SystemUserTokenProvider;
import no.nav.common.utils.Credentials;
import no.nav.common.utils.EnvironmentUtils;
import no.nav.veilarbvedtaksstotte.client.arena.VeilarbarenaClient;
import no.nav.veilarbvedtaksstotte.client.arena.VeilarbarenaClientImpl;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.DokarkivClient;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.DokarkivClientImpl;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.SafClient;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.SafClientImpl;
import no.nav.veilarbvedtaksstotte.client.dokdistfordeling.DokdistribusjonClient;
import no.nav.veilarbvedtaksstotte.client.dokdistfordeling.DokdistribusjonClientImpl;
import no.nav.veilarbvedtaksstotte.client.dokument.VeilarbdokumentClient;
import no.nav.veilarbvedtaksstotte.client.dokument.VeilarbdokumentClientImpl;
import no.nav.veilarbvedtaksstotte.client.egenvurdering.VeilarbvedtakinfoClient;
import no.nav.veilarbvedtaksstotte.client.egenvurdering.VeilarbvedtakinfoClientImpl;
import no.nav.veilarbvedtaksstotte.client.person.VeilarbpersonClient;
import no.nav.veilarbvedtaksstotte.client.person.VeilarbpersonClientImpl;
import no.nav.veilarbvedtaksstotte.client.registrering.VeilarbregistreringClient;
import no.nav.veilarbvedtaksstotte.client.registrering.VeilarbregistreringClientImpl;
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.VeilarboppfolgingClient;
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.VeilarboppfolgingClientImpl;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.VeilarbveilederClient;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.VeilarbveilederClientImpl;
import no.nav.veilarbvedtaksstotte.service.AuthService;
import no.nav.veilarbvedtaksstotte.service.UnleashService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static no.nav.common.utils.UrlUtils.*;
import static no.nav.veilarbvedtaksstotte.config.ApplicationConfig.APPLICATION_NAME;

@Configuration
public class ClientConfig {

    @Bean
    public VeilarbarenaClient arenaClient(AuthContextHolder authContextHolder) {
        return new VeilarbarenaClientImpl(
                naisPreprodOrNaisAdeoIngress("veilarbarena", true),
                authContextHolder
        );
    }

    @Bean
    public VeilarbdokumentClient dokumentClient(AuthContextHolder authContextHolder) {
        return new VeilarbdokumentClientImpl(
                naisPreprodOrNaisAdeoIngress("veilarbdokument", true),
                authContextHolder
        );
    }

    @Bean
    public VeilarbvedtakinfoClient egenvurderingClient(AuthContextHolder authContextHolder) {
        return new VeilarbvedtakinfoClientImpl(
                naisPreprodOrNaisAdeoIngress("veilarbvedtakinfo", true),
                authContextHolder
        );
    }

    @Bean
    public VeilarboppfolgingClient oppfolgingClient(AuthService authService, SystemUserTokenProvider systemUserTokenProvider) {
        return new VeilarboppfolgingClientImpl(
                naisPreprodOrNaisAdeoIngress("veilarboppfolging", true),
                authService::getInnloggetBrukerToken,
                systemUserTokenProvider::getSystemUserToken
        );
    }

    @Bean
    public VeilarbpersonClient personClient(AuthService authService) {
        return new VeilarbpersonClientImpl(naisPreprodOrNaisAdeoIngress("veilarbperson", true), authService::getInnloggetBrukerToken);
    }

    @Bean
    public VeilarbregistreringClient registreringClient(AuthContextHolder authContextHolder) {
        return new VeilarbregistreringClientImpl(
                naisPreprodOrNaisAdeoIngress("veilarbperson", true),
                authContextHolder
        );
    }

    @Bean
    public SafClient safClient(AuthContextHolder authContextHolder) {
        return new SafClientImpl(
                naisPreprodOrNaisAdeoIngress("saf", false),
                authContextHolder
        );
    }

    @Bean
    public VeilarbveilederClient veilederOgEnhetClient(AuthContextHolder authContextHolder) {
        return new VeilarbveilederClientImpl(
                naisPreprodOrNaisAdeoIngress("veilarbveileder", true),
                authContextHolder
        );
    }

    @Bean
    public DokarkivClient dokarkivClient(SystemUserTokenProvider systemUserTokenProvider, AuthContextHolder authContextHolder) {
        return new DokarkivClientImpl(
                naisPreprodOrNaisAdeoIngress("dokarkiv", false),
                systemUserTokenProvider,
                authContextHolder
        );
    }

    @Bean
    public DokdistribusjonClient dokDistribusjonClient(AuthContextHolder authContextHolder) {
        return new DokdistribusjonClientImpl(
                naisPreprodOrNaisAdeoIngress("dokdistfordeling", false),
                authContextHolder
        );
    }

    @Bean
    public PdlClient pdlClient(SystemUserTokenProvider systemUserTokenProvider) {
        String pdlUrl = internalDevOrProdIngress("pdl-api");

        return new PdlClientImpl(
                pdlUrl,
                systemUserTokenProvider::getSystemUserToken,
                systemUserTokenProvider::getSystemUserToken);
    }

    @Bean
    public AktorOppslagClient aktorOppslagClient(
            EnvironmentProperties properties,
            UnleashService unleashService,
            SystemUserTokenProvider systemUserTokenProvider,
            PdlClient pdlClient
    ) {

        PdlAktorOppslagClient pdlAktorOppslagClient = new PdlAktorOppslagClient(pdlClient);

        AktorregisterHttpClient aktorregisterClient = new AktorregisterHttpClient(
                properties.getAktorregisterUrl(), APPLICATION_NAME, systemUserTokenProvider::getSystemUserToken
        );

        return new CachedAktorOppslagClient(
                new ToggledAktorOppslagClient(aktorregisterClient, pdlAktorOppslagClient, unleashService::isPdlAktorOppslagEnabled)
        );
    }

    @Bean
    public UnleashClient unleashClient(EnvironmentProperties properties) {
        return new UnleashClientImpl(properties.getUnleashUrl(), APPLICATION_NAME);
    }

    @Bean
    public MetricsClient influxMetricsClient() {
        return new InfluxClient();
    }

    @Bean
    public LeaderElectionClient leaderElectionClient() {
        return new LeaderElectionHttpClient();
    }

    @Bean
    public Norg2Client norg2Client(EnvironmentProperties properties) {
        return new CachedNorg2Client(new NorgHttp2Client(properties.getNorg2Url()));
    }

    @Bean
    public AbacClient abacClient(EnvironmentProperties properties, Credentials serviceUserCredentials) {
        return new AbacCachedClient(new AbacHttpClient(properties.getAbacUrl(), serviceUserCredentials.username, serviceUserCredentials.password));
    }

    private static String naisPreprodOrNaisAdeoIngress(String appName, boolean withAppContextPath) {
        return EnvironmentUtils.isDevelopment().orElse(false)
                ? createNaisPreprodIngressUrl(appName, "q1", withAppContextPath)
                : createNaisAdeoIngressUrl(appName, withAppContextPath);
    }

    private static String internalDevOrProdIngress(String appName) {
        return EnvironmentUtils.isDevelopment().orElse(false)
                ? createDevInternalIngressUrl(appName)
                : createProdInternalIngressUrl(appName);
    }
}
