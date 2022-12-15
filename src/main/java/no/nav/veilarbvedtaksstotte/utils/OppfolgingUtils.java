package no.nav.veilarbvedtaksstotte.utils;

import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.OppfolgingPeriodeDTO;

import java.util.List;
import java.util.Optional;

public class OppfolgingUtils {

    public static boolean erSykmeldtUtenArbeidsgiver(String kvalifiseringsgruppe) {
        return kvalifiseringsgruppe.equals("VURDU");
    }

    public static Optional<OppfolgingPeriodeDTO> hentSisteOppfolgingsPeriode(List<OppfolgingPeriodeDTO> oppfolgingPerioder) {
        return oppfolgingPerioder.stream().min((o1, o2) -> {
            if (o1.sluttDato == null) {
                return -1;
            }

            if (o2.sluttDato == null) {
                return 1;
            }

            if (o1.sluttDato.isAfter(o2.sluttDato)) {
                return -1;
            }

            if (o1.sluttDato.isBefore(o2.sluttDato)) {
                return 1;
            }

            return 0;
        });
    }
}
