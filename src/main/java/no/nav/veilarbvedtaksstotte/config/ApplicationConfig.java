package no.nav.veilarbvedtaksstotte.config;

import no.nav.common.abac.Pep;
import no.nav.common.abac.VeilarbPep;
import no.nav.common.aktorregisterklient.AktorregisterHttpKlient;
import no.nav.common.aktorregisterklient.AktorregisterKlient;
import no.nav.common.aktorregisterklient.CachedAktorregisterKlient;
import no.nav.common.featuretoggle.UnleashService;
import no.nav.common.metrics.InfluxClient;
import no.nav.common.metrics.MetricsClient;
import no.nav.common.metrics.SensuConfig;
import no.nav.common.nais.NaisUtils;
import no.nav.common.sts.NaisSystemUserTokenProvider;
import no.nav.common.sts.SystemUserTokenProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

import static no.nav.common.featuretoggle.UnleashServiceConfig.resolveFromEnvironment;
import static no.nav.common.nais.NaisUtils.getCredentials;

@Profile("!local")
@Configuration
@EnableScheduling
@EnableConfigurationProperties(EnvironmentProperties.class)
public class ApplicationConfig {

    public final static String APPLICATION_NAME = "veilarbvedtaksstotte";

    public final static String KAFKA_BROKERS_URL_PROPERTY = "KAFKA_BROKERS_URL";

    private final String serviceUsername;

    private final String servicePassword;

    public ApplicationConfig() {
        NaisUtils.Credentials serviceUser = getCredentials("service_user");
        this.serviceUsername = serviceUser.username;
        this.servicePassword = serviceUser.password;
    }

    // TODO: Do this on startup
//    String dbUrl = getRequiredProperty(DatabaseConfig.VEILARBVEDTAKSSTOTTE_DB_URL_PROPERTY);
//        DbUtils.migrateAndClose(DbUtils.createDataSource(dbUrl, DbRole.ADMIN), DbRole.ADMIN);
//        ServletUtil.filterBuilder(new ToggleFilter(unleashService)).register(servletContext);

    @Bean
    public UnleashService unleashService() {
        return new UnleashService(resolveFromEnvironment());
    }

    @Bean
    public MetricsClient influxMetricsClient() {
        return new InfluxClient(SensuConfig.resolveNaisConfig());
    }

    @Bean
    public SystemUserTokenProvider systemUserTokenProvider(EnvironmentProperties properties) {
        return new NaisSystemUserTokenProvider(properties.getStsDiscoveryUrl(), serviceUsername, servicePassword);
    }

    @Bean
    public AktorregisterKlient aktorregisterKlient(EnvironmentProperties properties, SystemUserTokenProvider tokenProvider) {
        AktorregisterKlient aktorregisterKlient = new AktorregisterHttpKlient(
                properties.getAktorregisterUrl(), APPLICATION_NAME, tokenProvider::getSystemUserToken
        );
        return new CachedAktorregisterKlient(aktorregisterKlient);
    }

    @Bean
    public Pep veilarbPep(EnvironmentProperties properties) {
        return new VeilarbPep(properties.getAbacUrl(), serviceUsername, servicePassword);
    }

}
