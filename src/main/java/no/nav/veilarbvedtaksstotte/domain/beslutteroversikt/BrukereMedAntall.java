package no.nav.veilarbvedtaksstotte.domain.beslutteroversikt;

import lombok.Value;
import no.nav.veilarbvedtaksstotte.domain.beslutteroversikt.BeslutteroversiktBruker;

import java.util.List;

@Value
public class BrukereMedAntall {

    List<BeslutteroversiktBruker> brukere;

    long totaltAntall;

}
