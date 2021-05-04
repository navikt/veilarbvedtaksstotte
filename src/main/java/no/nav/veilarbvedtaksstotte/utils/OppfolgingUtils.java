package no.nav.veilarbvedtaksstotte.utils;

import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.OppfolgingPeriodeDTO;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public class OppfolgingUtils {

    public static boolean erSykmeldtUtenArbeidsgiver(String servicegruppe) {
        return servicegruppe.equals("VURDU");
    }

    public static Optional<ZonedDateTime> getOppfolgingStartDato(List<OppfolgingPeriodeDTO> oppfolgingPerioder) {
        return oppfolgingPerioder.stream()
                .filter(oppfolgingPeriode -> oppfolgingPeriode.getSluttDato() == null)
                .map(OppfolgingPeriodeDTO::getStartDato)
                .findFirst();
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

    public static boolean erDatoInnenforOppfolgingsperiode(ZonedDateTime dato,
                                                           OppfolgingPeriodeDTO oppfolgingPeriode) {
        return !dato.isBefore(oppfolgingPeriode.getStartDato()) &&
                (oppfolgingPeriode.getSluttDato() == null || !dato.isAfter(oppfolgingPeriode.getSluttDato()));

    }

    public static boolean erOppfolgingsperiodeAktiv(OppfolgingPeriodeDTO oppfolgingPeriode) {
        ZonedDateTime now = ZonedDateTime.now();
        return !oppfolgingPeriode.getStartDato().isAfter(now) &&
                (oppfolgingPeriode.getSluttDato() == null || !oppfolgingPeriode.getSluttDato().isBefore(now));
    }
}
