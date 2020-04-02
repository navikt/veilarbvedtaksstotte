package no.nav.veilarbvedtaksstotte.domain;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.veilarbvedtaksstotte.domain.enums.BeslutteroversiktStatus;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class BeslutteroversiktBruker {
    long vedtakId;
    String brukerFornavn;
    String brukerEtternavn;
    String brukerOppfolgingsenhetNavn;
    String brukerOppfolgingsenhetId;
    String brukerFnr;
    LocalDateTime vedtakStartet;
    BeslutteroversiktStatus status;
    String beslutterNavn;
    String beslutterIdent;
    String veilederNavn;
}
