package no.nav.veilarbvedtaksstotte.service;

import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Hovedmal;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsgruppe;
import no.nav.veilarbvedtaksstotte.domain.vedtak.VedtakStatus;
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

    private static VedtakService vedtakService = new VedtakService(
            null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null
    );

    @Test
    public void skal_ikke_feile_pa_gyldig_vedtak() {
        Vedtak vedtak = new Vedtak();
        vedtak.setVedtakStatus(VedtakStatus.UTKAST);
        vedtak.setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS);
        vedtak.setBegrunnelse("Begrunnelse");
        vedtak.setHovedmal(Hovedmal.SKAFFE_ARBEID);
        vedtak.setOpplysninger(Arrays.asList("opplysning 1", "opplysning 2"));

        vedtakService.validerVedtakForFerdigstillingOgUtsending(vedtak, null);
    }

    @Test
    public void skal_feile_hvis_vedtak_status_ikke_er_utkast() {
        exceptionRule.expectMessage("Vedtak har feil status, forventet status UTKAST");

        Vedtak vedtak = new Vedtak();
        vedtak.setVedtakStatus(VedtakStatus.SENDT);

        vedtakService.validerVedtakForFerdigstillingOgUtsending(vedtak, null);
    }

    @Test
    public void skal_feile_hvis_vedtak_mangler_innsatsgruppe() {
        exceptionRule.expectMessage("Vedtak mangler innsatsgruppe");

        Vedtak vedtak = new Vedtak();
        vedtak.setVedtakStatus(VedtakStatus.UTKAST);
        vedtak.setBegrunnelse("Begrunnelse");
        vedtak.setHovedmal(Hovedmal.SKAFFE_ARBEID);
        vedtak.setOpplysninger(Arrays.asList("opplysning 1", "opplysning 2"));

        vedtakService.validerVedtakForFerdigstillingOgUtsending(vedtak, null);
    }

    @Test
    public void skal_feile_hvis_beslutterprosess_startet_men_ikke_godkjent() {
        exceptionRule.expectMessage("Vedtak er ikke godkjent av beslutter");

        Vedtak vedtak = new Vedtak();
        vedtak.setVedtakStatus(VedtakStatus.UTKAST);
        vedtak.setInnsatsgruppe(Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS);
        vedtak.setBeslutterProsessStatus(KLAR_TIL_BESLUTTER);
        vedtak.setBeslutterIdent(TEST_BESLUTTER_IDENT);

        vedtakService.validerVedtakForFerdigstillingOgUtsending(vedtak, null);
    }

    @Test
    public void skal_feile_hvis_vedtak_med_gradert_varig_mangler_beslutter() {
        exceptionRule.expectMessage("Vedtak kan ikke bli sendt uten beslutter");

        Vedtak vedtak = new Vedtak();
        vedtak.setVedtakStatus(VedtakStatus.UTKAST);
        vedtak.setInnsatsgruppe(Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS);
        vedtak.setBegrunnelse("Begrunnelse");
        vedtak.setHovedmal(Hovedmal.SKAFFE_ARBEID);
        vedtak.setOpplysninger(Arrays.asList("opplysning 1", "opplysning 2"));

        vedtakService.validerVedtakForFerdigstillingOgUtsending(vedtak, null);
    }

    @Test
    public void skal_feile_hvis_vedtak_med_varig_mangler_beslutter() {
        exceptionRule.expectMessage("Vedtak kan ikke bli sendt uten beslutter");

        Vedtak vedtak = new Vedtak();
        vedtak.setVedtakStatus(VedtakStatus.UTKAST);
        vedtak.setInnsatsgruppe(Innsatsgruppe.VARIG_TILPASSET_INNSATS);
        vedtak.setBegrunnelse("Begrunnelse");
        vedtak.setHovedmal(Hovedmal.SKAFFE_ARBEID);
        vedtak.setOpplysninger(Arrays.asList("opplysning 1", "opplysning 2"));

        vedtakService.validerVedtakForFerdigstillingOgUtsending(vedtak, null);
    }

    @Test
    public void skal_feile_hvis_vedtak_mangler_opplysninger() {
        exceptionRule.expectMessage("Vedtak mangler opplysninger");

        Vedtak vedtak = new Vedtak();
        vedtak.setVedtakStatus(VedtakStatus.UTKAST);
        vedtak.setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS);
        vedtak.setBegrunnelse("Begrunnelse");
        vedtak.setHovedmal(Hovedmal.SKAFFE_ARBEID);

        vedtakService.validerVedtakForFerdigstillingOgUtsending(vedtak, null);
    }

    @Test
    public void skal_feile_hvis_vedtak_mangler_hovedmal() {
        exceptionRule.expectMessage("Vedtak mangler hovedmål");

        Vedtak vedtak = new Vedtak();
        vedtak.setVedtakStatus(VedtakStatus.UTKAST);
        vedtak.setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS);
        vedtak.setBegrunnelse("Begrunnelse");
        vedtak.setOpplysninger(Arrays.asList("opplysning 1", "opplysning 2"));

        vedtakService.validerVedtakForFerdigstillingOgUtsending(vedtak, null);
    }

    @Test
    public void skal_feile_hvis_vedtak_har_hovedmal_for_varig() {
        exceptionRule.expectMessage("Vedtak med varig tilpasset innsats skal ikke ha hovedmål");

        Vedtak vedtak = new Vedtak();
        vedtak.setVedtakStatus(VedtakStatus.UTKAST);
        vedtak.setInnsatsgruppe(Innsatsgruppe.VARIG_TILPASSET_INNSATS);
        vedtak.setBegrunnelse("Begrunnelse");
        vedtak.setHovedmal(Hovedmal.SKAFFE_ARBEID);
        vedtak.setBeslutterProsessStatus(GODKJENT_AV_BESLUTTER);
        vedtak.setOpplysninger(Arrays.asList("opplysning 1", "opplysning 2"));
        vedtak.setBeslutterIdent(TEST_BESLUTTER_IDENT);

        vedtakService.validerVedtakForFerdigstillingOgUtsending(vedtak, null);
    }

    @Test
    public void skal_feile_hvis_vedtak_manger_begrunelse() {
        exceptionRule.expectMessage("Vedtak mangler begrunnelse");

        Vedtak vedtak = new Vedtak();
        vedtak.setVedtakStatus(VedtakStatus.UTKAST);
        vedtak.setInnsatsgruppe(Innsatsgruppe.SITUASJONSBESTEMT_INNSATS);
        vedtak.setHovedmal(Hovedmal.SKAFFE_ARBEID);
        vedtak.setOpplysninger(Arrays.asList("opplysning 1", "opplysning 2"));

        vedtakService.validerVedtakForFerdigstillingOgUtsending(vedtak, null);
    }

    @Test
    public void skal_ikke_feile_hvis_vedtak_manger_begrunelse_og_er_standard() {
        Vedtak vedtak = new Vedtak();
        vedtak.setVedtakStatus(VedtakStatus.UTKAST);
        vedtak.setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS);
        vedtak.setHovedmal(Hovedmal.SKAFFE_ARBEID);
        vedtak.setOpplysninger(Arrays.asList("opplysning 1", "opplysning 2"));

        vedtakService.validerVedtakForFerdigstillingOgUtsending(vedtak, null);
    }

    @Test
    public void skal_feile_hvis_vedtak_manger_begrunelse_og_er_standard_og_gjeldende_er_varig() {
        exceptionRule.expectMessage("Vedtak mangler begrunnelse siden gjeldende vedtak er varig");

        Vedtak vedtak = new Vedtak();
        vedtak.setVedtakStatus(VedtakStatus.UTKAST);
        vedtak.setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS);
        vedtak.setHovedmal(Hovedmal.SKAFFE_ARBEID);
        vedtak.setOpplysninger(Arrays.asList("opplysning 1", "opplysning 2"));

        Vedtak gjeldendeVedtak = new Vedtak();
        gjeldendeVedtak.setInnsatsgruppe(Innsatsgruppe.VARIG_TILPASSET_INNSATS);

        vedtakService.validerVedtakForFerdigstillingOgUtsending(vedtak, gjeldendeVedtak);
    }

    @Test
    public void skal_feile_hvis_vedtak_allerede_er_distribuert_til_bruker() {
        exceptionRule.expectMessage("Vedtak er allerede distribuert til bruker");

        Vedtak vedtak = new Vedtak();
        vedtak.setVedtakStatus(VedtakStatus.UTKAST);
        vedtak.setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS);
        vedtak.setBegrunnelse("Begrunnelse");
        vedtak.setHovedmal(Hovedmal.SKAFFE_ARBEID);
        vedtak.setOpplysninger(Arrays.asList("opplysning 1", "opplysning 2"));
        vedtak.setDokumentbestillingId("123");

        vedtakService.validerVedtakForFerdigstillingOgUtsending(vedtak, null);
    }

    @Test
    public void skal_feile_hvis_vedtak_allerede_har_journalpost_id() {
        exceptionRule.expectMessage("Vedtak er allerede journalført");

        Vedtak vedtak = new Vedtak();
        vedtak.setVedtakStatus(VedtakStatus.UTKAST);
        vedtak.setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS);
        vedtak.setBegrunnelse("Begrunnelse");
        vedtak.setHovedmal(Hovedmal.SKAFFE_ARBEID);
        vedtak.setOpplysninger(Arrays.asList("opplysning 1", "opplysning 2"));
        vedtak.setJournalpostId("123");

        vedtakService.validerVedtakForFerdigstillingOgUtsending(vedtak, null);
    }

    @Test
    public void skal_feile_hvis_vedtak_allerede_har_dokument_info_id() {
        exceptionRule.expectMessage("Vedtak er allerede journalført");

        Vedtak vedtak = new Vedtak();
        vedtak.setVedtakStatus(VedtakStatus.UTKAST);
        vedtak.setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS);
        vedtak.setBegrunnelse("Begrunnelse");
        vedtak.setHovedmal(Hovedmal.SKAFFE_ARBEID);
        vedtak.setOpplysninger(Arrays.asList("opplysning 1", "opplysning 2"));
        vedtak.setDokumentInfoId("123");

        vedtakService.validerVedtakForFerdigstillingOgUtsending(vedtak, null);
    }
}
