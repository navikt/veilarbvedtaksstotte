package no.nav.veilarbvedtaksstotte.domain;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.veilarbvedtaksstotte.domain.enums.KafkaTopic;

@Data
@Accessors(chain = true)
public class FeiletKafkaMelding {
    long id;
    KafkaTopic topic;
    String key;
    String jsonPayload;
}
