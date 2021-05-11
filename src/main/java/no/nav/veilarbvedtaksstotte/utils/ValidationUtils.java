package no.nav.veilarbvedtaksstotte.utils;

import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsgruppe;
import no.nav.veilarbvedtaksstotte.domain.vedtak.UtkastetVedtak;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak;

import java.util.List;

import static no.nav.veilarbvedtaksstotte.domain.vedtak.BeslutterProsessStatus.GODKJENT_AV_BESLUTTER;
import static no.nav.veilarbvedtaksstotte.utils.InnsatsgruppeUtils.skalHaBeslutter;

public class ValidationUtils {

    public static final String VEDTAK_HAR_FEIL_STATUS_FORVENTET_STATUS_UTKAST = "Vedtak har feil status, forventet status UTKAST";
    public static final String VEDTAK_MANGLER_INNSATSGRUPPE = "Vedtak mangler innsatsgruppe";
    public static final String VEDTAK_KAN_IKKE_BLI_SENDT_UTEN_BESLUTTER = "Vedtak kan ikke bli sendt uten beslutter";
    public static final String VEDTAK_ER_IKKE_GODKJENT_AV_BESLUTTER = "Vedtak er ikke godkjent av beslutter";
    public static final String VEDTAK_MANGLER_OPPLYSNINGER = "Vedtak mangler opplysninger";
    public static final String VEDTAK_MANGLER_HOVEDMAAL = "Vedtak mangler hovedmål";
    public static final String VEDTAK_MED_VARIG_TILPASSET_INNSATS_SKAL_IKKE_HA_HOVEDMAAL = "Vedtak med varig tilpasset innsats skal ikke ha hovedmål";
    public static final String VEDTAK_MANGLER_BEGRUNNELSE_SIDEN_GJELDENDE_VEDTAK_ER_VARIG = "Vedtak mangler begrunnelse siden gjeldende vedtak er varig";
    public static final String VEDTAK_MANGLER_BEGRUNNELSE = "Vedtak mangler begrunnelse";

    public static <T> boolean isNull(T anyObject) {
        return anyObject == null;
    }

    public static boolean isNullOrEmpty(List list) {
        return isNull(list) || list.isEmpty();
    }

    public static boolean isNullOrEmpty(String str) {
        return isNull(str) || str.trim().isEmpty();
    }

    public static void validerVedtakForFerdigstillingOgUtsending(Vedtak vedtak, Vedtak gjeldendeVedtak) {

        if (!(vedtak instanceof UtkastetVedtak)) {
            throw new IllegalStateException(VEDTAK_HAR_FEIL_STATUS_FORVENTET_STATUS_UTKAST);
        }

        validerOpplysninger(vedtak);

        validerHovedmaalOgInnsatsgruppe((UtkastetVedtak) vedtak);

        validerVedtakInnsatsGruppeMotGjeldendeInnsatsgruppe(vedtak, gjeldendeVedtak);
    }

    private static void validerVedtakInnsatsGruppeMotGjeldendeInnsatsgruppe(Vedtak vedtak, Vedtak gjeldendeVedtak) {
        boolean harIkkeBegrunnelse = ValidationUtils.isNullOrEmpty(vedtak.getBegrunnelse());
        boolean erStandard = Innsatsgruppe.STANDARD_INNSATS.equals(vedtak.getInnsatsgruppe());
        boolean erGjeldendeVedtakVarig =
                !ValidationUtils.isNull(gjeldendeVedtak) &&
                        (Innsatsgruppe.VARIG_TILPASSET_INNSATS.equals(gjeldendeVedtak.getInnsatsgruppe()) ||
                                Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS.equals(gjeldendeVedtak.getInnsatsgruppe()));

        if (harIkkeBegrunnelse && erStandard && erGjeldendeVedtakVarig) {
            throw new IllegalStateException(VEDTAK_MANGLER_BEGRUNNELSE_SIDEN_GJELDENDE_VEDTAK_ER_VARIG);
        } else if (harIkkeBegrunnelse && !erStandard) {
            throw new IllegalStateException(VEDTAK_MANGLER_BEGRUNNELSE);
        }
    }

    private static void validerOpplysninger(Vedtak vedtak) {
        if (ValidationUtils.isNullOrEmpty(vedtak.getOpplysninger())) {
            throw new IllegalStateException(VEDTAK_MANGLER_OPPLYSNINGER);
        }
    }

    private static void validerHovedmaalOgInnsatsgruppe(UtkastetVedtak vedtak) {
        Innsatsgruppe innsatsgruppe = vedtak.getInnsatsgruppe();
        if (innsatsgruppe == null) {
            throw new IllegalStateException(VEDTAK_MANGLER_INNSATSGRUPPE);
        }

        boolean isGodkjentAvBeslutter = GODKJENT_AV_BESLUTTER.equals(vedtak.getBeslutterProsessStatus());

        if (skalHaBeslutter(innsatsgruppe)) {
            if (ValidationUtils.isNullOrEmpty(vedtak.getBeslutterIdent())) {
                throw new IllegalStateException(VEDTAK_KAN_IKKE_BLI_SENDT_UTEN_BESLUTTER);
            } else if (!isGodkjentAvBeslutter) {
                throw new IllegalStateException(VEDTAK_ER_IKKE_GODKJENT_AV_BESLUTTER);
            }
        }

        boolean manglerHovedmal = ValidationUtils.isNull(vedtak.getHovedmal()) && !Innsatsgruppe.VARIG_TILPASSET_INNSATS.equals(innsatsgruppe);
        if (manglerHovedmal) {
            throw new IllegalStateException(VEDTAK_MANGLER_HOVEDMAAL);
        } else {
            boolean harVarigTilpassetHovedmal = !ValidationUtils.isNull(vedtak.getHovedmal()) && Innsatsgruppe.VARIG_TILPASSET_INNSATS.equals(innsatsgruppe);
            if (harVarigTilpassetHovedmal) {
                throw new IllegalStateException(VEDTAK_MED_VARIG_TILPASSET_INNSATS_SKAL_IKKE_HA_HOVEDMAAL);
            }
        }
    }

}
