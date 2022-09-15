package no.nav.veilarbvedtaksstotte.config;

import no.nav.common.abac.AbacCachedClient;
import no.nav.common.abac.AbacClient;
import no.nav.common.abac.AbacHttpClient;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.client.aktoroppslag.CachedAktorOppslagClient;
import no.nav.common.client.aktoroppslag.PdlAktorOppslagClient;
import no.nav.common.client.pdl.PdlClientImpl;
import no.nav.common.featuretoggle.UnleashClient;
import no.nav.common.featuretoggle.UnleashClientImpl;
import no.nav.common.job.leader_election.LeaderElectionClient;
import no.nav.common.job.leader_election.LeaderElectionHttpClient;
import no.nav.common.metrics.InfluxClient;
import no.nav.common.metrics.MetricsClient;
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient;
import no.nav.common.utils.Credentials;
import no.nav.common.utils.EnvironmentUtils;
import no.nav.common.utils.UrlUtils;
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
import no.nav.veilarbvedtaksstotte.service.OboContexService;
import no.nav.veilarbvedtaksstotte.utils.DownstreamApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Supplier;

import static no.nav.common.utils.UrlUtils.*;
import static no.nav.veilarbvedtaksstotte.config.ApplicationConfig.APPLICATION_NAME;

@Configuration
public class ClientConfig {

    @Bean
    public VeilarbarenaClient arenaClient(OboContexService oboContexService) {
        String clientCluster = isProduction() ? "prod-fss" : "dev-fss";
        Supplier<String> userTokenSupplier = oboContexService.userTokenSupplier(
                DownstreamAPIs.getVeilarbarena().invoke(clientCluster)
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
    public VeilarbvedtakinfoClient egenvurderingClient(OboContexService oboContexService) {
        String clientCluster = isProduction() ? "prod-fss" : "dev-fss";
        Supplier<String> userTokenSupplier = oboContexService.userTokenSupplier(
                DownstreamAPIs.getVeilarbvedtakinfo().invoke(clientCluster)
        );
        return new VeilarbvedtakinfoClientImpl(
                naisPreprodOrNaisAdeoIngress("veilarbvedtakinfo", true),
                userTokenSupplier
        );
    }

    @Bean
    public VeilarboppfolgingClient oppfolgingClient(OboContexService oboContexService, AzureAdMachineToMachineTokenClient tokenClient) {
        DownstreamApi veilarboppfolging = DownstreamAPIs.getVeilarboppfolging().invoke(isProduction() ? "prod-fss" : "dev-fss");
        Supplier<String> userTokenSupplier = oboContexService.userTokenSupplier(veilarboppfolging);

        String url = UrlUtils.createServiceUrl(veilarboppfolging.serviceName, veilarboppfolging.namespace, true);
        return new VeilarboppfolgingClientImpl(url, userTokenSupplier,
                () -> tokenClient.createMachineToMachineToken(tokenScope(veilarboppfolging))
        );
    }

    @Bean
    public VeilarbpersonClient personClient(OboContexService oboContexService) {
        String clientCluster = isProduction() ? "prod-fss" : "dev-fss";
        Supplier<String> userTokenSupplier = oboContexService.userTokenSupplier(
                DownstreamAPIs.getVeilarbperson().invoke(clientCluster)
        );
        return new VeilarbpersonClientImpl(naisPreprodOrNaisAdeoIngress("veilarbperson", true), userTokenSupplier);
    }

    @Bean
    public VeilarbregistreringClient registreringClient(OboContexService oboContexService) {
        String clientCluster = isProduction() ? "prod-fss" : "dev-fss";
        Supplier<String> userTokenSupplier = oboContexService.userTokenSupplier(
                DownstreamAPIs.getVeilarbperson().invoke(clientCluster)
        );
        return new VeilarbregistreringClientImpl(
                naisPreprodOrNaisAdeoIngress("veilarbperson", true),
                userTokenSupplier
        );
    }

    @Bean
    public SafClient safClient(OboContexService oboContexService) {
        DownstreamApi safClient = DownstreamAPIs.getSaf().invoke(isProduction() ? "prod-fss" : "dev-fss");
        Supplier<String> userTokenSupplier = oboContexService.userTokenSupplier(safClient);
        return new SafClientImpl(
                naisPreprodOrNaisAdeoIngress(safClient.serviceName, false),
                userTokenSupplier
        );
    }

    @Bean
    public VeilarbveilederClient veilederOgEnhetClient(AuthContextHolder authContextHolder, OboContexService oboContexService) {
        DownstreamApi veilarbveileder = DownstreamAPIs.getVeilarbveileder().invoke(isProduction() ? "prod-fss" : "dev-fss");
        Supplier<String> userTokenSupplier = oboContexService.userTokenSupplier(veilarbveileder);
        return new VeilarbveilederClientImpl(
                UrlUtils.createServiceUrl(veilarbveileder.serviceName, veilarbveileder.namespace, true),
                authContextHolder,
                userTokenSupplier
        );
    }

    @Bean
    public DokarkivClient dokarkivClient(OboContexService oboContexService) {
        DownstreamApi dokarkivClient = DownstreamAPIs.getDokarkiv().invoke(isProduction() ? "prod-fss" : "dev-fss");
        Supplier<String> userTokenSupplier = oboContexService.userTokenSupplier(dokarkivClient);
        String url = isProduction()
                ? createProdInternalIngressUrl(dokarkivClient.serviceName)
                : createDevInternalIngressUrl(dokarkivClient.serviceName);
        return new DokarkivClientImpl(
                url,
                userTokenSupplier
        );
    }

    @Bean
    public RegoppslagClient regoppslagClient(AzureAdMachineToMachineTokenClient tokenClient) {
        DownstreamApi regoppslag = DownstreamAPIs.getRegoppslag().invoke(isProduction() ? "prod-fss" : "dev-fss");
        String url = isProduction()
                ? createProdInternalIngressUrl(regoppslag.serviceName)
                : createDevInternalIngressUrl(regoppslag.serviceName);

        return new RegoppslagClientImpl(url, () -> tokenClient.createMachineToMachineToken(tokenScope(regoppslag)));
    }

    @Bean
    public DokdistribusjonClient dokDistribusjonClient(AzureAdMachineToMachineTokenClient tokenClient) {
        String appName = isProduction() ? "dokdistfordeling" : "dokdistfordeling-q1";
        String url = isProduction()
                ? createProdInternalIngressUrl(appName)
                : createDevInternalIngressUrl(appName);

        String clientCluster = isProduction() ? "prod-fss" : "dev-fss";
        String tokenScope = String.format("api://%s.teamdokumenthandtering.saf/.default", clientCluster);

        return new DokdistribusjonClientImpl(url, () -> tokenClient.createMachineToMachineToken(tokenScope));
    }

    @Bean
    public AktorOppslagClient aktorOppslagClient(AzureAdMachineToMachineTokenClient tokenClient) {
        DownstreamApi pdl = DownstreamAPIs.getPdl().invoke(isProduction() ? "prod-fss" : "dev-fss");
        String pdlUrl = isProduction()
                ? createProdInternalIngressUrl(pdl.serviceName)
                : createDevInternalIngressUrl(pdl.serviceName);

        PdlClientImpl pdlClient = new PdlClientImpl(pdlUrl, () -> tokenClient.createMachineToMachineToken(tokenScope(pdl)));
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

    private static boolean isProduction() {
        return EnvironmentUtils.isProduction().orElseThrow();
    }

    private static String naisPreprodOrNaisAdeoIngress(String appName, boolean withAppContextPath) {
        return isProduction()
                ? createNaisAdeoIngressUrl(appName, withAppContextPath)
                : createNaisPreprodIngressUrl(appName, "q1", withAppContextPath);
    }

    private static String tokenScope(DownstreamApi downstreamApi){
        return String.format("api://%s.%s.%s/.default", downstreamApi.cluster, downstreamApi.namespace, downstreamApi.serviceName);
    }
}
