package no.nav.veilarbvedtaksstotte.config;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.abac.*;
import no.nav.common.abac.audit.AuditLogger;
import no.nav.common.abac.audit.NimbusSubjectProvider;
import no.nav.common.abac.audit.SpringAuditRequestInfoSupplier;
import no.nav.common.client.pdl.AktorOppslagClient;
import no.nav.common.client.pdl.CachedAktorOppslagClient;
import no.nav.common.client.pdl.PdlAktorOppslagClient;
import no.nav.common.featuretoggle.UnleashService;
import no.nav.common.leaderelection.LeaderElectionClient;
import no.nav.common.leaderelection.LeaderElectionHttpClient;
import no.nav.common.metrics.InfluxClient;
import no.nav.common.metrics.MetricsClient;
import no.nav.common.sts.NaisSystemUserTokenProvider;
import no.nav.common.sts.SystemUserTokenProvider;
import no.nav.common.utils.Credentials;
import no.nav.common.utils.EnvironmentUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import static no.nav.common.featuretoggle.UnleashServiceConfig.resolveFromEnvironment;
import static no.nav.common.utils.NaisUtils.getCredentials;
import static no.nav.common.utils.UrlUtils.createDevAdeoIngressUrl;
import static no.nav.common.utils.UrlUtils.createNaisAdeoIngressUrl;

@Slf4j
@Configuration
@EnableScheduling
@EnableConfigurationProperties({EnvironmentProperties.class})
public class ApplicationConfig {

    public final static String APPLICATION_NAME = "veilarbvedtaksstotte";

    @Bean
    public Credentials serviceUserCredentials() {
        return getCredentials("service_user");
    }

    @Bean
    public UnleashService unleashService() {
        return new UnleashService(resolveFromEnvironment());
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
    public SystemUserTokenProvider systemUserTokenProvider(EnvironmentProperties properties, Credentials serviceUserCredentials) {
        return new NaisSystemUserTokenProvider(properties.getStsDiscoveryUrl(), serviceUserCredentials.username, serviceUserCredentials.password);
    }

    @Bean
    public AktorOppslagClient aktorOppslagClient(SystemUserTokenProvider tokenProvider) {
        String pdlUrl = EnvironmentUtils.isDevelopment().orElse(false)
                ? createDevAdeoIngressUrl("pdl", false)
                : createNaisAdeoIngressUrl("pdl", false);

        return new CachedAktorOppslagClient(
                new PdlAktorOppslagClient(pdlUrl, tokenProvider::getSystemUserToken, tokenProvider::getSystemUserToken)
        );
    }

    @Bean
    public AbacClient abacClient(EnvironmentProperties properties, Credentials serviceUserCredentials) {
        return new AbacCachedClient(new AbacHttpClient(properties.getAbacUrl(), serviceUserCredentials.username, serviceUserCredentials.password));
    }

    @Bean
    public Pep veilarbPep(Credentials serviceUserCredentials, AbacClient abacClient) {
        return new VeilarbPep(
                serviceUserCredentials.username, abacClient,
                new AuditLogger(), new NimbusSubjectProvider(),
                new SpringAuditRequestInfoSupplier()
        );
    }

}
