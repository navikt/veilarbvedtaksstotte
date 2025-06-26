package no.nav.veilarbvedtaksstotte.client.pdf

import no.nav.common.health.HealthCheck

interface PdfClient : HealthCheck {
    fun genererPdf(brevdata: BrevdataDto): ByteArray
    fun genererOyeblikksbildeCvPdf(cvOyeblikksbildeData: CvInnholdMedMottakerDto): ByteArray
    fun genererOyeblikksbildeEgenVurderingPdf(egenvurderingOyeblikksbildeData: EgenvurderingMedMottakerDto): ByteArray
    fun genererOyeblikksbildeArbeidssokerRegistretPdf(registreringOyeblikksbildeData: OpplysningerOmArbeidssoekerMedProfileringMedMottakerDto): ByteArray

}
