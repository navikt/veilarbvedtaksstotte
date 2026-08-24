package no.nav.veilarbvedtaksstotte.client.pdf

import io.getunleash.DefaultUnleash
import no.nav.common.health.HealthCheckResult
import no.nav.veilarbvedtaksstotte.utils.NY_PDFGENERATOR_SKRUDD_PAA

class TogglePdfClient(
    val oboPdfClient: PdfClient,
    val ptoPdfClient: PdfClient,
    val unleashService: DefaultUnleash,
) : PdfClient {

    private fun aktivClient(): PdfClient =
        if (unleashService.isEnabled(NY_PDFGENERATOR_SKRUDD_PAA)) oboPdfClient
        else ptoPdfClient

    override fun genererPdf(brevdata: BrevdataDto): ByteArray =
        aktivClient().genererPdf(brevdata)

    override fun genererOyeblikksbildeCvPdf(cvOyeblikksbildeData: CvInnholdMedMottakerDto): ByteArray =
        aktivClient().genererOyeblikksbildeCvPdf(cvOyeblikksbildeData)

    override fun genererOyeblikksbildeEgenVurderingPdf(egenvurderingOyeblikksbildeData: EgenvurderingMedMottakerDto): ByteArray =
        aktivClient().genererOyeblikksbildeEgenVurderingPdf(egenvurderingOyeblikksbildeData)

    override fun genererOyeblikksbildeArbeidssokerRegistretPdf(registreringOyeblikksbildeData: OpplysningerOmArbeidssoekerMedProfileringMedMottakerDto): ByteArray =
        aktivClient().genererOyeblikksbildeArbeidssokerRegistretPdf(registreringOyeblikksbildeData)

    override fun checkHealth(): HealthCheckResult =
        aktivClient().checkHealth()
}
