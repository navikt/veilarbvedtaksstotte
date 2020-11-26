package no.nav.veilarbvedtaksstotte.utils;

import no.nav.veilarbvedtaksstotte.client.oppfolging.OppfolgingPeriodeDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class OppfolgingUtils {

    public static boolean erSykmeldtUtenArbeidsgiver(String servicegruppe) {
        return servicegruppe.equals("VURDU");
    }

    public static Optional<LocalDateTime> getOppfolgingStartDato(List<OppfolgingPeriodeDTO> oppfolgingPerioder) {
        return oppfolgingPerioder.stream()
                .filter(oppfolgingPeriode -> oppfolgingPeriode.getSluttDato() == null)
                .map(OppfolgingPeriodeDTO::getStartDato)
                .findFirst();
    }
}
