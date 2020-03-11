package no.nav.veilarbvedtaksstotte.utils;

import no.nav.veilarbvedtaksstotte.domain.OppfolgingPeriodeDTO;
import no.nav.veilarbvedtaksstotte.domain.enums.Innsatsgruppe;

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

    public static Innsatsgruppe utledInnsatsgruppe(String servicegruppe) {
        switch (servicegruppe) {
            case "IKVAL":
                return Innsatsgruppe.STANDARD_INNSATS;
            case "VARIG":
                return Innsatsgruppe.VARIG_TILPASSET_INNSATS;
            case "BFORM":
                return Innsatsgruppe.SITUASJONSBESTEMT_INNSATS;
            case "BATT":
                return Innsatsgruppe.SPESIELT_TILPASSET_INNSATS;
            default:
                throw new IllegalArgumentException("Ugyldig servicegruppe " + servicegruppe);
        }
    }
}
