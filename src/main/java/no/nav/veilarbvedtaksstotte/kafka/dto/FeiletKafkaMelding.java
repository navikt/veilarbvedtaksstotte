package no.nav.veilarbvedtaksstotte.kafka.dto;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.veilarbvedtaksstotte.kafka.KafkaTopic;

@Data
@Accessors(chain = true)
public class FeiletKafkaMelding {
    long id;
    KafkaTopic topic;
    String key;
    String jsonPayload;
}
