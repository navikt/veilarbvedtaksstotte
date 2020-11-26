package no.nav.veilarbvedtaksstotte.domain.beslutteroversikt;

import lombok.Data;
import lombok.experimental.Accessors;

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
    LocalDateTime statusEndret;
    String beslutterNavn;
    String beslutterIdent;
    String veilederNavn;
}
