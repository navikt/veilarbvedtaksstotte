package no.nav.fo.veilarbvedtaksstotte.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;
@Data
@Accessors(chain = true)

public class KafkaAvsluttOppfolging {
   String aktoerId;
   Date sluttdato;
}
