package no.nav.veilarbvedtaksstotte.config;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

@Data
@Accessors(chain = true)
public class KafkaAvroContext {
    Map<String, ?> config;

    public Map<String, ?> getConfig() {
        return this.config;
    }
}
