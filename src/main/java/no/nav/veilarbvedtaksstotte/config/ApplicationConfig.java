package no.nav.veilarbvedtaksstotte.config;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.abac.*;
import no.nav.common.aktorregisterklient.AktorregisterHttpKlient;
import no.nav.common.aktorregisterklient.AktorregisterKlient;
import no.nav.common.aktorregisterklient.CachedAktorregisterKlient;
import no.nav.common.featuretoggle.UnleashService;
import no.nav.common.metrics.InfluxClient;
import no.nav.common.metrics.MetricsClient;
import no.nav.common.metrics.SensuConfig;
import no.nav.common.sts.NaisSystemUserTokenProvider;
import no.nav.common.sts.SystemUserTokenProvider;
import no.nav.common.utils.Credentials;
import no.nav.veilarbvedtaksstotte.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

import static no.nav.common.featuretoggle.UnleashServiceConfig.resolveFromEnvironment;
import static no.nav.common.utils.NaisUtils.getCredentials;

@Profile("!local")
@Slf4j
@Configuration
@EnableScheduling
@EnableConfigurationProperties({EnvironmentProperties.class, KafkaProperties.class})
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
        return new InfluxClient(SensuConfig.resolveNaisConfig());
    }

    @Bean
    public SystemUserTokenProvider systemUserTokenProvider(EnvironmentProperties properties, Credentials serviceUserCredentials) {
        return new NaisSystemUserTokenProvider(properties.getStsDiscoveryUrl(), serviceUserCredentials.username, serviceUserCredentials.password);
    }

    @Bean
    public AktorregisterKlient aktorregisterKlient(EnvironmentProperties properties, SystemUserTokenProvider tokenProvider) {
        AktorregisterKlient aktorregisterKlient = new AktorregisterHttpKlient(
                properties.getAktorregisterUrl(), APPLICATION_NAME, tokenProvider::getSystemUserToken
        );
        return new CachedAktorregisterKlient(aktorregisterKlient);
    }

    @Bean
    public AbacClient abacClient(EnvironmentProperties properties, Credentials serviceUserCredentials) {
        return new AbacCachedClient(new AbacHttpClient(properties.getAbacUrl(), serviceUserCredentials.username, serviceUserCredentials.password));
    }

    @Bean
    public Pep veilarbPep(Credentials serviceUserCredentials, AbacClient abacClient) {
        return new VeilarbPep(serviceUserCredentials.username, abacClient, new AuditLogger());
    }

}
