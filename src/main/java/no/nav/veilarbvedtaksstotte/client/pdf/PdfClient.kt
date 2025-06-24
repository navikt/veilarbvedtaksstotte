package no.nav.veilarbvedtaksstotte.client.pdf

import no.nav.common.health.HealthCheck
import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.client.arbeidssoekeregisteret.OpplysningerOmArbeidssoekerMedProfilering
import no.nav.veilarbvedtaksstotte.client.dokument.MalType
import no.nav.veilarbvedtaksstotte.client.person.dto.CvInnhold
import no.nav.veilarbvedtaksstotte.domain.Malform
import no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.EgenvurderingDto

interface PdfClient : HealthCheck {
    fun genererPdf(brevdata: Brevdata): ByteArray
    fun genererOyeblikksbildeCvPdf(cvOyeblikksbildeData: CvInnhold): ByteArray
    fun genererOyeblikksbildeEgenVurderingPdf(egenvurderingOyeblikksbildeData: EgenvurderingDto): ByteArray
    fun genererOyeblikksbildeArbeidssokerRegistretPdf(registreringOyeblikksbildeData: OpplysningerOmArbeidssoekerMedProfilering): ByteArray


    data class Brevdata(
        val malType: MalType,
        val veilederNavn: String,
        val navKontor: String,
        val dato: String,
        val malform: Malform,
        val begrunnelse: List<String>,
        val kilder: List<String>,
        val mottaker: Mottaker,
        val utkast: Boolean,
        val ungdomsgaranti: Boolean
    )

    data class Mottaker(
        val navn: String,
        val fodselsnummer: Fnr,
    )
}
