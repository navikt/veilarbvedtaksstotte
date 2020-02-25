package no.nav.fo.veilarbvedtaksstotte.utils;

import no.nav.fo.veilarbvedtaksstotte.domain.OppfolgingPeriodeDTO;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.Innsatsgruppe;

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

    public static Innsatsgruppe utledInnsatsgruppe(String servicegruppe, String formidlingsgruppe) {
        // servicegruppe: "IKVAL"
        // formidlingsgruppe: "ARBS"
        // rettighetsgruppe: "DAGP"
        // TODO: Utled riktig innsatsgruppe

        return Innsatsgruppe.STANDARD_INNSATS;
    }
}
