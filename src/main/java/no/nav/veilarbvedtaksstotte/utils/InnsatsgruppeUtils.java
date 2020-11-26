package no.nav.veilarbvedtaksstotte.utils;

import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsgruppe;

public class InnsatsgruppeUtils {

    public static boolean skalHaBeslutter(Innsatsgruppe innsatsgruppe) {
        return Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS == innsatsgruppe
                || Innsatsgruppe.VARIG_TILPASSET_INNSATS == innsatsgruppe;
    }

}
