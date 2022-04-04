package no.nav.veilarbvedtaksstotte.service

import no.nav.veilarbvedtaksstotte.domain.vedtak.*
import no.nav.veilarbvedtaksstotte.service.VedtakService.validerVedtakForFerdigstilling
import no.nav.veilarbvedtaksstotte.utils.TestData
import no.nav.veilarbvedtaksstotte.utils.TestUtils.assertThrowsWithMessage
import org.junit.Test
import java.util.*

class VedtakServiceValiderVedtakForFerdigstillingOgUtsendingTest {

    @Test
    fun skal_ikke_feile_pa_gyldig_vedtak() {
        val vedtak = Vedtak()
        vedtak.vedtakStatus = VedtakStatus.UTKAST
        vedtak.innsatsgruppe = Innsatsgruppe.STANDARD_INNSATS
        vedtak.begrunnelse = "Begrunnelse"
        vedtak.hovedmal = Hovedmal.SKAFFE_ARBEID
        vedtak.opplysninger = Arrays.asList("opplysning 1", "opplysning 2")
        validerVedtakForFerdigstilling(vedtak, null)
    }

    @Test
    fun skal_feile_hvis_vedtak_status_ikke_er_utkast() {
        val vedtak = Vedtak()
        vedtak.vedtakStatus = VedtakStatus.SENDT

        assertThrowsWithMessage<IllegalStateException>("Vedtak har feil status, forventet status UTKAST") {
            validerVedtakForFerdigstilling(vedtak, null)
        }
    }

    @Test
    fun skal_feile_hvis_vedtak_mangler_innsatsgruppe() {
        val vedtak = Vedtak()
        vedtak.vedtakStatus = VedtakStatus.UTKAST
        vedtak.begrunnelse = "Begrunnelse"
        vedtak.hovedmal = Hovedmal.SKAFFE_ARBEID
        vedtak.opplysninger = Arrays.asList("opplysning 1", "opplysning 2")

        assertThrowsWithMessage<IllegalStateException>("Vedtak mangler innsatsgruppe") {
            validerVedtakForFerdigstilling(vedtak, null)
        }
    }

    @Test
    fun skal_feile_hvis_beslutterprosess_startet_men_ikke_godkjent() {
        val vedtak = Vedtak()
        vedtak.vedtakStatus = VedtakStatus.UTKAST
        vedtak.innsatsgruppe = Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS
        vedtak.beslutterProsessStatus = BeslutterProsessStatus.KLAR_TIL_BESLUTTER
        vedtak.beslutterIdent = TestData.TEST_BESLUTTER_IDENT

        assertThrowsWithMessage<IllegalStateException>("Vedtak er ikke godkjent av beslutter") {
            validerVedtakForFerdigstilling(vedtak, null)
        }
    }

    @Test
    fun skal_feile_hvis_vedtak_med_gradert_varig_mangler_beslutter() {
        val vedtak = Vedtak()
        vedtak.vedtakStatus = VedtakStatus.UTKAST
        vedtak.innsatsgruppe = Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS
        vedtak.begrunnelse = "Begrunnelse"
        vedtak.hovedmal = Hovedmal.SKAFFE_ARBEID
        vedtak.opplysninger = Arrays.asList("opplysning 1", "opplysning 2")

        assertThrowsWithMessage<IllegalStateException>("Vedtak kan ikke bli sendt uten beslutter") {
            validerVedtakForFerdigstilling(vedtak, null)
        }
    }

    @Test
    fun skal_feile_hvis_vedtak_med_varig_mangler_beslutter() {
        val vedtak = Vedtak()
        vedtak.vedtakStatus = VedtakStatus.UTKAST
        vedtak.innsatsgruppe = Innsatsgruppe.VARIG_TILPASSET_INNSATS
        vedtak.begrunnelse = "Begrunnelse"
        vedtak.hovedmal = Hovedmal.SKAFFE_ARBEID
        vedtak.opplysninger = Arrays.asList("opplysning 1", "opplysning 2")

        assertThrowsWithMessage<IllegalStateException>("Vedtak kan ikke bli sendt uten beslutter") {
            validerVedtakForFerdigstilling(vedtak, null)
        }
    }

    @Test
    fun skal_feile_hvis_vedtak_mangler_opplysninger() {
        val vedtak = Vedtak()
        vedtak.vedtakStatus = VedtakStatus.UTKAST
        vedtak.innsatsgruppe = Innsatsgruppe.STANDARD_INNSATS
        vedtak.begrunnelse = "Begrunnelse"
        vedtak.hovedmal = Hovedmal.SKAFFE_ARBEID

        assertThrowsWithMessage<IllegalStateException>("Vedtak mangler opplysninger") {
            validerVedtakForFerdigstilling(vedtak, null)
        }
    }

    @Test
    fun skal_feile_hvis_vedtak_mangler_hovedmal() {
        val vedtak = Vedtak()
        vedtak.vedtakStatus = VedtakStatus.UTKAST
        vedtak.innsatsgruppe = Innsatsgruppe.STANDARD_INNSATS
        vedtak.begrunnelse = "Begrunnelse"
        vedtak.opplysninger = Arrays.asList("opplysning 1", "opplysning 2")

        assertThrowsWithMessage<IllegalStateException>("Vedtak mangler hovedmål") {
            validerVedtakForFerdigstilling(vedtak, null)
        }
    }

    @Test
    fun skal_feile_hvis_vedtak_har_hovedmal_for_varig() {
        val vedtak = Vedtak()
        vedtak.vedtakStatus = VedtakStatus.UTKAST
        vedtak.innsatsgruppe = Innsatsgruppe.VARIG_TILPASSET_INNSATS
        vedtak.begrunnelse = "Begrunnelse"
        vedtak.hovedmal = Hovedmal.SKAFFE_ARBEID
        vedtak.beslutterProsessStatus = BeslutterProsessStatus.GODKJENT_AV_BESLUTTER
        vedtak.opplysninger = Arrays.asList("opplysning 1", "opplysning 2")
        vedtak.beslutterIdent = TestData.TEST_BESLUTTER_IDENT

        assertThrowsWithMessage<IllegalStateException>("Vedtak med varig tilpasset innsats skal ikke ha hovedmål") {
            validerVedtakForFerdigstilling(vedtak, null)
        }
    }

    @Test
    fun skal_feile_hvis_vedtak_manger_begrunelse() {
        val vedtak = Vedtak()
        vedtak.vedtakStatus = VedtakStatus.UTKAST
        vedtak.innsatsgruppe = Innsatsgruppe.SITUASJONSBESTEMT_INNSATS
        vedtak.hovedmal = Hovedmal.SKAFFE_ARBEID
        vedtak.opplysninger = Arrays.asList("opplysning 1", "opplysning 2")

        assertThrowsWithMessage<IllegalStateException>("Vedtak mangler begrunnelse") {
            validerVedtakForFerdigstilling(vedtak, null)
        }
    }

    @Test
    fun skal_ikke_feile_hvis_vedtak_manger_begrunelse_og_er_standard() {
        val vedtak = Vedtak()
        vedtak.vedtakStatus = VedtakStatus.UTKAST
        vedtak.innsatsgruppe = Innsatsgruppe.STANDARD_INNSATS
        vedtak.hovedmal = Hovedmal.SKAFFE_ARBEID
        vedtak.opplysninger = Arrays.asList("opplysning 1", "opplysning 2")

        validerVedtakForFerdigstilling(vedtak, null)
    }

    @Test
    fun skal_feile_hvis_vedtak_manger_begrunelse_og_er_standard_og_gjeldende_er_varig() {
        val vedtak = Vedtak()
        vedtak.vedtakStatus = VedtakStatus.UTKAST
        vedtak.innsatsgruppe = Innsatsgruppe.STANDARD_INNSATS
        vedtak.hovedmal = Hovedmal.SKAFFE_ARBEID
        vedtak.opplysninger = Arrays.asList("opplysning 1", "opplysning 2")
        val gjeldendeVedtak = Vedtak()
        gjeldendeVedtak.innsatsgruppe = Innsatsgruppe.VARIG_TILPASSET_INNSATS

        assertThrowsWithMessage<IllegalStateException>("Vedtak mangler begrunnelse siden gjeldende vedtak er varig") {
            validerVedtakForFerdigstilling(vedtak, gjeldendeVedtak)
        }
    }

    @Test
    fun skal_feile_hvis_vedtak_allerede_har_journalpost_id() {
        val vedtak = Vedtak()
        vedtak.vedtakStatus = VedtakStatus.UTKAST
        vedtak.innsatsgruppe = Innsatsgruppe.STANDARD_INNSATS
        vedtak.begrunnelse = "Begrunnelse"
        vedtak.hovedmal = Hovedmal.SKAFFE_ARBEID
        vedtak.opplysninger = Arrays.asList("opplysning 1", "opplysning 2")
        vedtak.journalpostId = "123"

        assertThrowsWithMessage<IllegalStateException>("Vedtak er allerede journalført") {
            validerVedtakForFerdigstilling(vedtak, null)
        }
    }

    @Test
    fun skal_feile_hvis_vedtak_allerede_har_dokument_info_id() {
        val vedtak = Vedtak()
        vedtak.vedtakStatus = VedtakStatus.UTKAST
        vedtak.innsatsgruppe = Innsatsgruppe.STANDARD_INNSATS
        vedtak.begrunnelse = "Begrunnelse"
        vedtak.hovedmal = Hovedmal.SKAFFE_ARBEID
        vedtak.opplysninger = Arrays.asList("opplysning 1", "opplysning 2")
        vedtak.dokumentInfoId = "123"

        assertThrowsWithMessage<IllegalStateException>("Vedtak er allerede journalført") {
            validerVedtakForFerdigstilling(vedtak, null)
        }
    }
}
