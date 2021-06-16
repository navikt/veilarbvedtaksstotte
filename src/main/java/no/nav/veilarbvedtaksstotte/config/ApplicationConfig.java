package no.nav.veilarbvedtaksstotte.config;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.abac.AbacClient;
import no.nav.common.abac.Pep;
import no.nav.common.abac.VeilarbPep;
import no.nav.common.abac.audit.AuditConfig;
import no.nav.common.abac.audit.AuditLogger;
import no.nav.common.abac.audit.NimbusSubjectProvider;
import no.nav.common.abac.audit.SpringAuditRequestInfoSupplier;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.auth.context.AuthContextHolderThreadLocal;
import no.nav.common.sts.NaisSystemUserTokenProvider;
import no.nav.common.sts.SystemUserTokenProvider;
import no.nav.common.utils.Credentials;
import no.nav.veilarbvedtaksstotte.utils.JsonUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.annotation.PostConstruct;

import static no.nav.common.kafka.util.KafkaPropertiesPreset.onPremByteProducerProperties;
import static no.nav.common.kafka.util.KafkaPropertiesPreset.onPremDefaultConsumerProperties;
import static no.nav.common.utils.NaisUtils.getCredentials;

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
    public SystemUserTokenProvider systemUserTokenProvider(EnvironmentProperties properties, Credentials serviceUserCredentials) {
        return new NaisSystemUserTokenProvider(properties.getStsDiscoveryUrl(), serviceUserCredentials.username, serviceUserCredentials.password);
    }

    @Bean
    public Pep veilarbPep(Credentials serviceUserCredentials, AbacClient abacClient) {
        AuditConfig auditConfig = new AuditConfig(new AuditLogger(), new SpringAuditRequestInfoSupplier(), null);
        return new VeilarbPep(
                serviceUserCredentials.username, abacClient,
                new NimbusSubjectProvider(), auditConfig
        );
    }

    @Bean
    public AuthContextHolder authContextHolder() {
        return AuthContextHolderThreadLocal.instance();
    }

    @Bean
    public KafkaConfig.EnvironmentContext kafkaConfigEnvContext(KafkaProperties kafkaProperties,
                                                                Credentials credentials) {
        return new KafkaConfig.EnvironmentContext()
                .setConsumerClientProperties(
                        onPremDefaultConsumerProperties(
                                KafkaConfig.CONSUMER_GROUP_ID, kafkaProperties.getBrokersUrl(), credentials
                        )
                )
                .setProducerClientProperties(
                        onPremByteProducerProperties(
                                KafkaConfig.PRODUCER_CLIENT_ID, kafkaProperties.getBrokersUrl(), credentials
                        )
                );
    }

    @PostConstruct
    public void initJsonUtils() {
        JsonUtils.init();
    }
}
