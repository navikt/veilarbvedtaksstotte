package no.nav.veilarbvedtaksstotte.service;

import no.nav.veilarbvedtaksstotte.domain.vedtak.*;
import no.nav.veilarbvedtaksstotte.utils.ValidationUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Arrays;

import static no.nav.veilarbvedtaksstotte.domain.vedtak.BeslutterProsessStatus.GODKJENT_AV_BESLUTTER;
import static no.nav.veilarbvedtaksstotte.domain.vedtak.BeslutterProsessStatus.KLAR_TIL_BESLUTTER;
import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_BESLUTTER_IDENT;

public class VedtakServiceValiderVedtakForFerdigstillingOgUtsendingTest {

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    public void skal_ikke_feile_pa_gyldig_vedtak() {
        UtkastetVedtak utkastetVedtak = UtkastetVedtak.builder()
                .vedtakStatus(VedtakStatus.UTKAST)
                .innsatsgruppe(Innsatsgruppe.STANDARD_INNSATS)
                .begrunnelse("Begrunnelse")
                .hovedmal(Hovedmal.SKAFFE_ARBEID)
                .opplysninger(Arrays.asList("opplysning 1", "opplysning 2"))
                .build();

        ValidationUtils.validerVedtakForFerdigstillingOgUtsending(utkastetVedtak, null);
    }

    @Test
    public void skal_feile_hvis_vedtak_mangler_innsatsgruppe() {
        exceptionRule.expectMessage("Vedtak mangler innsatsgruppe");

        UtkastetVedtak utkastetVedtak = UtkastetVedtak.builder()
                .vedtakStatus(VedtakStatus.UTKAST)
                .begrunnelse("Begrunnelse")
                .hovedmal(Hovedmal.SKAFFE_ARBEID)
                .opplysninger(Arrays.asList("opplysning 1", "opplysning 2"))
                .build();

        ValidationUtils.validerVedtakForFerdigstillingOgUtsending(utkastetVedtak, null);
    }

    @Test
    public void skal_feile_hvis_beslutterprosess_startet_men_ikke_godkjent() {
        exceptionRule.expectMessage("Vedtak er ikke godkjent av beslutter");

        UtkastetVedtak utkastetVedtak = UtkastetVedtak.builder()
                .vedtakStatus(VedtakStatus.UTKAST)
                .innsatsgruppe(Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS)
                .beslutterProsessStatus(KLAR_TIL_BESLUTTER)
                .beslutterIdent(TEST_BESLUTTER_IDENT)
                .build();

        ValidationUtils.validerVedtakForFerdigstillingOgUtsending(utkastetVedtak, null);
    }

    @Test
    public void skal_feile_hvis_vedtak_med_gradert_varig_mangler_beslutter() {
        exceptionRule.expectMessage("Vedtak kan ikke bli sendt uten beslutter");

        UtkastetVedtak utkastetVedtak = UtkastetVedtak.builder()
                .vedtakStatus(VedtakStatus.UTKAST)
                .innsatsgruppe(Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS)
                .begrunnelse("Begrunnelse")
                .hovedmal(Hovedmal.SKAFFE_ARBEID)
                .opplysninger(Arrays.asList("opplysning 1", "opplysning 2"))
                .build();

        ValidationUtils.validerVedtakForFerdigstillingOgUtsending(utkastetVedtak, null);
    }

    @Test
    public void skal_feile_hvis_vedtak_med_varig_mangler_beslutter() {
        exceptionRule.expectMessage("Vedtak kan ikke bli sendt uten beslutter");

        UtkastetVedtak utkastetVedtak = UtkastetVedtak.builder()
                .vedtakStatus(VedtakStatus.UTKAST)
                .innsatsgruppe(Innsatsgruppe.VARIG_TILPASSET_INNSATS)
                .begrunnelse("Begrunnelse")
                .hovedmal(Hovedmal.SKAFFE_ARBEID)
                .opplysninger(Arrays.asList("opplysning 1", "opplysning 2"))
                .build();

        ValidationUtils.validerVedtakForFerdigstillingOgUtsending(utkastetVedtak, null);
    }

    @Test
    public void skal_feile_hvis_vedtak_mangler_opplysninger() {
        exceptionRule.expectMessage("Vedtak mangler opplysninger");

        UtkastetVedtak utkastetVedtak = UtkastetVedtak.builder()
                .vedtakStatus(VedtakStatus.UTKAST)
                .innsatsgruppe(Innsatsgruppe.STANDARD_INNSATS)
                .begrunnelse("Begrunnelse")
                .hovedmal(Hovedmal.SKAFFE_ARBEID)
                .build();

        ValidationUtils.validerVedtakForFerdigstillingOgUtsending(utkastetVedtak, null);
    }

    @Test
    public void skal_feile_hvis_vedtak_mangler_hovedmal() {
        exceptionRule.expectMessage("Vedtak mangler hovedmål");

        UtkastetVedtak utkastetVedtak = UtkastetVedtak.builder()
                .vedtakStatus(VedtakStatus.UTKAST)
                .innsatsgruppe(Innsatsgruppe.STANDARD_INNSATS)
                .begrunnelse("Begrunnelse")
                .opplysninger(Arrays.asList("opplysning 1", "opplysning 2"))
                .build();

        ValidationUtils.validerVedtakForFerdigstillingOgUtsending(utkastetVedtak, null);
    }

    @Test
    public void skal_feile_hvis_vedtak_har_hovedmal_for_varig() {
        exceptionRule.expectMessage("Vedtak med varig tilpasset innsats skal ikke ha hovedmål");

        UtkastetVedtak utkastetVedtak = UtkastetVedtak.builder()
                .vedtakStatus(VedtakStatus.UTKAST)
                .innsatsgruppe(Innsatsgruppe.VARIG_TILPASSET_INNSATS)
                .begrunnelse("Begrunnelse")
                .hovedmal(Hovedmal.SKAFFE_ARBEID)
                .beslutterProsessStatus(GODKJENT_AV_BESLUTTER)
                .opplysninger(Arrays.asList("opplysning 1", "opplysning 2"))
                .beslutterIdent(TEST_BESLUTTER_IDENT)
                .build();

        ValidationUtils.validerVedtakForFerdigstillingOgUtsending(utkastetVedtak, null);
    }

    @Test
    public void skal_feile_hvis_vedtak_manger_begrunelse() {
        exceptionRule.expectMessage("Vedtak mangler begrunnelse");

        UtkastetVedtak utkastetVedtak = UtkastetVedtak.builder()
                .vedtakStatus(VedtakStatus.UTKAST)
                .innsatsgruppe(Innsatsgruppe.SITUASJONSBESTEMT_INNSATS)
                .hovedmal(Hovedmal.SKAFFE_ARBEID)
                .opplysninger(Arrays.asList("opplysning 1", "opplysning 2"))
                .build();

        ValidationUtils.validerVedtakForFerdigstillingOgUtsending(utkastetVedtak, null);
    }

    @Test
    public void skal_ikke_feile_hvis_vedtak_manger_begrunelse_og_er_standard() {
        UtkastetVedtak utkastetVedtak = UtkastetVedtak.builder()
                .vedtakStatus(VedtakStatus.UTKAST)
                .innsatsgruppe(Innsatsgruppe.STANDARD_INNSATS)
                .hovedmal(Hovedmal.SKAFFE_ARBEID)
                .opplysninger(Arrays.asList("opplysning 1", "opplysning 2"))
                .build();

        ValidationUtils.validerVedtakForFerdigstillingOgUtsending(utkastetVedtak, null);
    }

    @Test
    public void skal_feile_hvis_vedtak_manger_begrunelse_og_er_standard_og_gjeldende_er_varig() {
        exceptionRule.expectMessage("Vedtak mangler begrunnelse siden gjeldende vedtak er varig");

        UtkastetVedtak utkastetVedtak = UtkastetVedtak.builder()
                .vedtakStatus(VedtakStatus.UTKAST)
                .innsatsgruppe(Innsatsgruppe.STANDARD_INNSATS)
                .hovedmal(Hovedmal.SKAFFE_ARBEID)
                .opplysninger(Arrays.asList("opplysning 1", "opplysning 2"))
                .build();

        FattetVedtak gjeldendeVedtak = FattetVedtak.builder()
                .innsatsgruppe(Innsatsgruppe.VARIG_TILPASSET_INNSATS)
                .build();

        ValidationUtils.validerVedtakForFerdigstillingOgUtsending(utkastetVedtak, gjeldendeVedtak);
    }
}
