package no.nav.veilarbvedtaksstotte.config;

import no.nav.common.abac.AbacCachedClient;
import no.nav.common.abac.AbacClient;
import no.nav.common.abac.AbacHttpClient;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.client.aktoroppslag.*;
import no.nav.common.client.pdl.PdlClientImpl;
import no.nav.common.featuretoggle.UnleashClient;
import no.nav.common.featuretoggle.UnleashClientImpl;
import no.nav.common.job.leader_election.LeaderElectionClient;
import no.nav.common.job.leader_election.LeaderElectionHttpClient;
import no.nav.common.metrics.InfluxClient;
import no.nav.common.metrics.MetricsClient;
import no.nav.common.sts.ServiceToServiceTokenProvider;
import no.nav.common.sts.SystemUserTokenProvider;
import no.nav.common.sts.utils.AzureAdServiceTokenProviderBuilder;
import no.nav.common.token_client.builder.AzureAdTokenClientBuilder;
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient;
import no.nav.common.utils.Credentials;
import no.nav.common.utils.EnvironmentUtils;
import no.nav.veilarbvedtaksstotte.client.DownstreamAPIs;
import no.nav.veilarbvedtaksstotte.client.arena.VeilarbarenaClient;
import no.nav.veilarbvedtaksstotte.client.arena.VeilarbarenaClientImpl;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.DokarkivClient;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.DokarkivClientImpl;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.SafClient;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.SafClientImpl;
import no.nav.veilarbvedtaksstotte.client.dokdistfordeling.DokdistribusjonClient;
import no.nav.veilarbvedtaksstotte.client.dokdistfordeling.DokdistribusjonClientImpl;
import no.nav.veilarbvedtaksstotte.client.egenvurdering.VeilarbvedtakinfoClient;
import no.nav.veilarbvedtaksstotte.client.egenvurdering.VeilarbvedtakinfoClientImpl;
import no.nav.veilarbvedtaksstotte.client.norg2.Norg2Client;
import no.nav.veilarbvedtaksstotte.client.norg2.Norg2ClientImpl;
import no.nav.veilarbvedtaksstotte.client.pdf.PdfClient;
import no.nav.veilarbvedtaksstotte.client.pdf.PdfClientImpl;
import no.nav.veilarbvedtaksstotte.client.person.VeilarbpersonClient;
import no.nav.veilarbvedtaksstotte.client.person.VeilarbpersonClientImpl;
import no.nav.veilarbvedtaksstotte.client.registrering.VeilarbregistreringClient;
import no.nav.veilarbvedtaksstotte.client.registrering.VeilarbregistreringClientImpl;
import no.nav.veilarbvedtaksstotte.client.regoppslag.RegoppslagClient;
import no.nav.veilarbvedtaksstotte.client.regoppslag.RegoppslagClientImpl;
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.VeilarboppfolgingClient;
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.VeilarboppfolgingClientImpl;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.VeilarbveilederClient;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.VeilarbveilederClientImpl;
import no.nav.veilarbvedtaksstotte.service.AuthService;
import no.nav.veilarbvedtaksstotte.service.ContextAwareService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Supplier;

import static no.nav.common.utils.EnvironmentUtils.requireClusterName;
import static no.nav.common.utils.UrlUtils.*;
import static no.nav.veilarbvedtaksstotte.config.ApplicationConfig.APPLICATION_NAME;

@Configuration
public class ClientConfig {

    @Bean
    public VeilarbarenaClient arenaClient(ContextAwareService contextAwareService) {
        String safCluster = isProduction() ? "prod-fss"  : "dev-fss";
        Supplier<String> userTokenSupplier = contextAwareService.contextAwareUserTokenSupplier(
                DownstreamAPIs.getArena().invoke(safCluster)
        );
        return new VeilarbarenaClientImpl(
                naisPreprodOrNaisAdeoIngress("veilarbarena", true),
                userTokenSupplier
        );
    }

    @Bean
    public PdfClient pdfClient() {
        return new PdfClientImpl(createServiceUrl("pto-pdfgen", false));
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
    public SafClient safClient(ContextAwareService contextAwareService) {
        String safCluster = isProduction() ? "prod-fss"  : "dev-fss";
        Supplier<String> userTokenSupplier = contextAwareService.contextAwareUserTokenSupplier(
                DownstreamAPIs.getSaf().invoke(safCluster)
        );
        return new SafClientImpl(
                naisPreprodOrNaisAdeoIngress("saf", false),
                userTokenSupplier
        );
    }

    @Bean
    public VeilarbveilederClient veilederOgEnhetClient(AuthContextHolder authContextHolder, ContextAwareService contextAwareService) {
        String safCluster = isProduction() ? "prod-fss"  : "dev-fss";
        Supplier<String> userTokenSupplier = contextAwareService.contextAwareUserTokenSupplier(
                DownstreamAPIs.getVeilarbveileder().invoke(safCluster)
        );
        return new VeilarbveilederClientImpl(
                naisPreprodOrNaisAdeoIngress("veilarbveileder", true),
                authContextHolder,
                userTokenSupplier
        );
    }

    @Bean
    public DokarkivClient dokarkivClient(SystemUserTokenProvider systemUserTokenProvider, AuthContextHolder authContextHolder) {
        String url = isProduction()
                ? createProdInternalIngressUrl("dokarkiv")
                : createDevInternalIngressUrl("dokarkiv-q1");

        return new DokarkivClientImpl(
                url,
                systemUserTokenProvider,
                authContextHolder
        );
    }

    @Bean
    public RegoppslagClient regoppslagClient(SystemUserTokenProvider systemUserTokenProvider) {
        String url = isProduction()
                ? createProdInternalIngressUrl("regoppslag")
                : createDevInternalIngressUrl("regoppslag-q1");

        return new RegoppslagClientImpl(url, systemUserTokenProvider);
    }

    @Bean
    public DokdistribusjonClient dokDistribusjonClient() {
        String url = isProduction()
                ? createProdInternalIngressUrl("dokdistfordeling")
                : createDevInternalIngressUrl("dokdistfordeling-q1");

        String safCluster = isProduction() ? "prod-fss"  : "dev-fss";

        Supplier<String> serviceTokenSupplier = () -> serviceToServiceTokenProvider()
                .getServiceToken("saf", "teamdokumenthandtering", safCluster);

        return new DokdistribusjonClientImpl(
                url,
                serviceTokenSupplier
        );
    }

    @Bean
    public ServiceToServiceTokenProvider serviceToServiceTokenProvider() {
        return AzureAdServiceTokenProviderBuilder.builder()
                .withEnvironmentDefaults()
                .build();
    }

    @Bean
    public AktorOppslagClient aktorOppslagClient(SystemUserTokenProvider systemUserTokenProvider) {
        String pdlUrl = isProduction()
                ? createProdInternalIngressUrl("pdl-api")
                : createDevInternalIngressUrl("pdl-api-q1");

        PdlClientImpl pdlClient = new PdlClientImpl(
                pdlUrl,
                systemUserTokenProvider::getSystemUserToken,
                systemUserTokenProvider::getSystemUserToken);

        return new CachedAktorOppslagClient(new PdlAktorOppslagClient(pdlClient));
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
        return new Norg2ClientImpl(properties.getNorg2Url());
    }

    @Bean
    public AbacClient abacClient(EnvironmentProperties properties, Credentials serviceUserCredentials) {
        return new AbacCachedClient(new AbacHttpClient(properties.getAbacUrl(), serviceUserCredentials.username, serviceUserCredentials.password));
    }

    @Bean
    public AzureAdOnBehalfOfTokenClient azureAdOnBehalfOfTokenClient() {
        return AzureAdTokenClientBuilder.builder()
                .withNaisDefaults()
                .buildOnBehalfOfTokenClient();
    }

    private static boolean isProduction() {
        return EnvironmentUtils.isProduction().orElseThrow();
    }

    private static String naisPreprodOrNaisAdeoIngress(String appName, boolean withAppContextPath) {
        return isProduction()
                ? createNaisAdeoIngressUrl(appName, withAppContextPath)
                : createNaisPreprodIngressUrl(appName, "q1", withAppContextPath);
    }
}
