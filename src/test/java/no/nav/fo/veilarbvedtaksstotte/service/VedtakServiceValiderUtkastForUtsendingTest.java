package no.nav.fo.veilarbvedtaksstotte.service;

import no.nav.fo.veilarbvedtaksstotte.domain.Vedtak;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.Hovedmal;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.Innsatsgruppe;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Arrays;

public class VedtakServiceValiderUtkastForUtsendingTest {

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    private static VedtakService vedtakService = new VedtakService(
            null, null,null,
            null,null,null,null,null,
            null,null,null
    );

    @Test
    public void skal_ikke_feile_pa_gyldig_vedtak() {
        Vedtak vedtak = new Vedtak();
        vedtak.setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS);
        vedtak.setBegrunnelse("Begrunnelse");
        vedtak.setHovedmal(Hovedmal.SKAFFE_ARBEID);
        vedtak.setOpplysninger(Arrays.asList("opplysning 1", "opplysning 2"));

        vedtakService.validerUtkastForUtsending(vedtak, null, null);
    }

    @Test
    public void skal_feile_hvis_vedtak_mangler_innsatsgruppe() {
        exceptionRule.expectMessage("Vedtak mangler innsatsgruppe");

        Vedtak vedtak = new Vedtak();
        vedtak.setBegrunnelse("Begrunnelse");
        vedtak.setHovedmal(Hovedmal.SKAFFE_ARBEID);
        vedtak.setOpplysninger(Arrays.asList("opplysning 1", "opplysning 2"));

        vedtakService.validerUtkastForUtsending(vedtak, null, null);
    }

    @Test
    public void skal_feile_hvis_vedtak_med_gradert_varig_mangler_beslutter() {
        exceptionRule.expectMessage("Vedtak kan ikke bli sendt uten beslutter");

        Vedtak vedtak = new Vedtak();
        vedtak.setInnsatsgruppe(Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS);
        vedtak.setBegrunnelse("Begrunnelse");
        vedtak.setHovedmal(Hovedmal.SKAFFE_ARBEID);
        vedtak.setOpplysninger(Arrays.asList("opplysning 1", "opplysning 2"));

        vedtakService.validerUtkastForUtsending(vedtak, null, null);
    }

    @Test
    public void skal_feile_hvis_vedtak_med_varig_mangler_beslutter() {
        exceptionRule.expectMessage("Vedtak kan ikke bli sendt uten beslutter");

        Vedtak vedtak = new Vedtak();
        vedtak.setInnsatsgruppe(Innsatsgruppe.VARIG_TILPASSET_INNSATS);
        vedtak.setBegrunnelse("Begrunnelse");
        vedtak.setHovedmal(Hovedmal.SKAFFE_ARBEID);
        vedtak.setOpplysninger(Arrays.asList("opplysning 1", "opplysning 2"));

        vedtakService.validerUtkastForUtsending(vedtak, null, null);
    }

    @Test
    public void skal_feile_hvis_vedtak_mangler_opplysninger() {
        exceptionRule.expectMessage("Vedtak mangler opplysninger");

        Vedtak vedtak = new Vedtak();
        vedtak.setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS);
        vedtak.setBegrunnelse("Begrunnelse");
        vedtak.setHovedmal(Hovedmal.SKAFFE_ARBEID);

        vedtakService.validerUtkastForUtsending(vedtak, null, null);
    }

    @Test
    public void skal_feile_hvis_vedtak_mangler_hovedmal() {
        exceptionRule.expectMessage("Vedtak mangler hovedmål");

        Vedtak vedtak = new Vedtak();
        vedtak.setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS);
        vedtak.setBegrunnelse("Begrunnelse");
        vedtak.setOpplysninger(Arrays.asList("opplysning 1", "opplysning 2"));

        vedtakService.validerUtkastForUtsending(vedtak, null, null);
    }

    @Test
    public void skal_feile_hvis_vedtak_har_hovedmal_for_varig() {
        exceptionRule.expectMessage("Vedtak med varig tilpasset innsats skal ikke ha hovedmål");

        Vedtak vedtak = new Vedtak();
        vedtak.setInnsatsgruppe(Innsatsgruppe.VARIG_TILPASSET_INNSATS);
        vedtak.setBegrunnelse("Begrunnelse");
        vedtak.setHovedmal(Hovedmal.SKAFFE_ARBEID);
        vedtak.setOpplysninger(Arrays.asList("opplysning 1", "opplysning 2"));

        vedtakService.validerUtkastForUtsending(vedtak, null, "beslutter");
    }

    @Test
    public void skal_feile_hvis_vedtak_manger_begrunelse() {
        exceptionRule.expectMessage("Vedtak mangler begrunnelse");

        Vedtak vedtak = new Vedtak();
        vedtak.setInnsatsgruppe(Innsatsgruppe.SITUASJONSBESTEMT_INNSATS);
        vedtak.setHovedmal(Hovedmal.SKAFFE_ARBEID);
        vedtak.setOpplysninger(Arrays.asList("opplysning 1", "opplysning 2"));

        vedtakService.validerUtkastForUtsending(vedtak, null, null);
    }

    @Test
    public void skal_ikke_feile_hvis_vedtak_manger_begrunelse_og_er_standard() {
        Vedtak vedtak = new Vedtak();
        vedtak.setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS);
        vedtak.setHovedmal(Hovedmal.SKAFFE_ARBEID);
        vedtak.setOpplysninger(Arrays.asList("opplysning 1", "opplysning 2"));

        vedtakService.validerUtkastForUtsending(vedtak, null, null);
    }

    @Test
    public void skal_feile_hvis_vedtak_manger_begrunelse_og_er_standard_og_gjeldende_er_varig() {
        exceptionRule.expectMessage("Vedtak mangler begrunnelse siden gjeldende vedtak er varig");

        Vedtak vedtak = new Vedtak();
        vedtak.setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS);
        vedtak.setHovedmal(Hovedmal.SKAFFE_ARBEID);
        vedtak.setOpplysninger(Arrays.asList("opplysning 1", "opplysning 2"));

        Vedtak gjeldendeVedtak = new Vedtak();
        gjeldendeVedtak.setInnsatsgruppe(Innsatsgruppe.VARIG_TILPASSET_INNSATS);

        vedtakService.validerUtkastForUtsending(vedtak, gjeldendeVedtak, null);
    }

}