package no.nav.veilarbvedtaksstotte.config;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Properties;

@Data
@Accessors(chain = true)
public class KafkaEnvironmentContext {
    Properties onPremConsumerClientProperties;
    Properties onPremProducerClientProperties;
    Properties aivenConsumerClientProperties;
    Properties aivenProducerClientProperties;
}
