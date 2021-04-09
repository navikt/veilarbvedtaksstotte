package no.nav.veilarbvedtaksstotte.repository.domain;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.common.types.identer.EnhetId;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class UtrulletEnhet {
    EnhetId enhetId;
    String navn;
    LocalDateTime createdAt;
}
