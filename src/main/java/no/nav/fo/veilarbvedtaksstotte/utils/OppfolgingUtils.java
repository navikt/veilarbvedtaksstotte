package no.nav.fo.veilarbvedtaksstotte.utils;

import no.nav.fo.veilarbvedtaksstotte.domain.OppfolgingPeriodeDTO;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class OppfolgingUtils {

    public static boolean erSykmeldtUtenArbeidsgiver(String servicegruppe) {
        return servicegruppe.equals("VURDU");
    }

    public static Date getOppfolgingStartDato(List<OppfolgingPeriodeDTO> oppfolgingPerioder) {
        return oppfolgingPerioder.stream()
                .filter(oppfolgingPeriode -> oppfolgingPeriode.getSluttDato() != null)
                .map(OppfolgingPeriodeDTO::getStartDato)
                .collect(Collectors.toList())
                .get(0);
    }
}
