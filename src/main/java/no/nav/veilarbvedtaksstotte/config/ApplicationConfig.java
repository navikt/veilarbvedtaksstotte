package no.nav.veilarbvedtaksstotte.config;

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig;
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
import no.nav.common.token_client.builder.AzureAdTokenClientBuilder;
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient;
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient;
import no.nav.common.utils.Credentials;
import no.nav.veilarbvedtaksstotte.utils.JsonUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG;
import static java.lang.String.format;
import static no.nav.common.kafka.util.KafkaEnvironmentVariables.*;
import static no.nav.common.kafka.util.KafkaPropertiesPreset.*;
import static no.nav.common.utils.EnvironmentUtils.getRequiredProperty;
import static no.nav.common.utils.NaisUtils.getCredentials;
import static no.nav.veilarbvedtaksstotte.config.KafkaConfig.PRODUCER_CLIENT_ID;

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
    public AzureAdMachineToMachineTokenClient azureAdMachineToMachineTokenClient() {
        return AzureAdTokenClientBuilder.builder()
                .withNaisDefaults()
                .buildMachineToMachineTokenClient();
    }

    @Bean
    public AzureAdOnBehalfOfTokenClient azureAdOnBehalfOfTokenClient() {
        return AzureAdTokenClientBuilder.builder()
                .withNaisDefaults()
                .buildOnBehalfOfTokenClient();
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
                .setOnPremConsumerClientProperties(
                        onPremDefaultConsumerProperties(
                                KafkaConfig.CONSUMER_GROUP_ID, kafkaProperties.getBrokersUrl(), credentials
                        )
                )
                .setOnPremProducerClientProperties(
                        onPremByteProducerProperties(
                                PRODUCER_CLIENT_ID, kafkaProperties.getBrokersUrl(), credentials
                        )
                )
                .setAivenProducerClientProperties(aivenByteProducerProperties(PRODUCER_CLIENT_ID));
    }

    @Bean
    public KafkaConfig.KafkaAvroContext kafkaAvroContext() {
        Map<String, Object> schemaRegistryConfig = new HashMap<>();
        schemaRegistryConfig.put(SCHEMA_REGISTRY_URL_CONFIG, getRequiredProperty(KAFKA_SCHEMA_REGISTRY));
        schemaRegistryConfig.put(SchemaRegistryClientConfig.BASIC_AUTH_CREDENTIALS_SOURCE, "USER_INFO");
        schemaRegistryConfig.put(SchemaRegistryClientConfig.USER_INFO_CONFIG,
                format("%s:%s",
                        getRequiredProperty(KAFKA_SCHEMA_REGISTRY_USER),
                        getRequiredProperty(KAFKA_SCHEMA_REGISTRY_PASSWORD))
        );

        return new KafkaConfig.KafkaAvroContext().setConfig(schemaRegistryConfig);
    }

    @PostConstruct
    public void initJsonUtils() {
        JsonUtils.init();
    }
}
