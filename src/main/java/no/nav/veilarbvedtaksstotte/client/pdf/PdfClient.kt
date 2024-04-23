package no.nav.veilarbvedtaksstotte.client.pdf

import no.nav.common.health.HealthCheck
import no.nav.veilarbvedtaksstotte.client.dokument.MalType
import no.nav.veilarbvedtaksstotte.client.norg2.EnhetPostadresse
import no.nav.veilarbvedtaksstotte.client.norg2.EnhetPostboksadresse
import no.nav.veilarbvedtaksstotte.client.norg2.EnhetStedsadresse
import no.nav.veilarbvedtaksstotte.client.person.dto.CvInnhold
import no.nav.veilarbvedtaksstotte.client.registrering.dto.RegistreringResponseDto
import no.nav.veilarbvedtaksstotte.domain.Målform
import no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.OyeblikksbildeEgenvurderingDto

interface PdfClient : HealthCheck {
    fun genererPdf(brevdata: Brevdata): ByteArray
    fun genererOyeblikksbildeCvPdf(cvOyeblikksbildeData: CvInnhold): ByteArray
    fun genererOyeblikksbildeRegistreringPdf(registreringOyeblikksbildeData: RegistreringResponseDto): ByteArray
    fun genererOyeblikksbildeEgenVurderingPdf(egenvurderingOyeblikksbildeData: OyeblikksbildeEgenvurderingDto): ByteArray


    data class Brevdata(
        val malType: MalType,
        val veilederNavn: String,
        val navKontor: String,
        val kontaktEnhetNavn: String,
        val kontaktTelefonnummer: String,
        val dato: String,
        val malform: Målform,
        val begrunnelse: List<String>,
        val kilder: List<String>,
        val mottaker: Mottaker,
        val postadresse: Adresse,
        val utkast: Boolean
    )

    data class Mottaker(
        val navn: String,
        val adresselinje1: String,
        val adresselinje2: String?,
        val adresselinje3: String?,
        val postnummer: String?,
        val poststed: String?,
        val land: String?
    )

    data class Adresse(
        val adresselinje: String,
        val postnummer: String,
        val poststed: String,
    ) {
        companion object {
            fun fraEnhetPostadresse(enhetPostadresse: EnhetPostadresse): Adresse {
                when (enhetPostadresse) {
                    is EnhetPostboksadresse ->
                        return Adresse(
                            adresselinje =
                            "Postboks ${enhetPostadresse.postboksnummer ?: ""} ${enhetPostadresse.postboksanlegg ?: ""}",
                            postnummer = enhetPostadresse.postnummer,
                            poststed = enhetPostadresse.poststed
                        )

                    is EnhetStedsadresse ->
                        return Adresse(
                            adresselinje =
                            "${enhetPostadresse.gatenavn ?: ""} ${enhetPostadresse.husnummer ?: ""}${enhetPostadresse.husbokstav ?: ""}",
                            postnummer = enhetPostadresse.postnummer,
                            poststed = enhetPostadresse.poststed,
                        )

                    else -> throw IllegalStateException("Manglende mapping for enhetPostadresse")
                }
            }
        }

    }
}
