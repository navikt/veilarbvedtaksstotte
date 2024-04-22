package no.nav.veilarbvedtaksstotte.service

import no.nav.common.client.norg2.Enhet
import no.nav.common.types.identer.EnhetId
import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.client.dokument.ProduserDokumentDTO
import no.nav.veilarbvedtaksstotte.client.norg2.EnhetKontaktinformasjon
import no.nav.veilarbvedtaksstotte.client.pdf.PdfClient
import no.nav.veilarbvedtaksstotte.client.person.VeilarbpersonClient
import no.nav.veilarbvedtaksstotte.client.person.dto.CvInnhold
import no.nav.veilarbvedtaksstotte.client.registrering.dto.RegistreringsdataDto
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.VeilarbveilederClient
import no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.OyeblikksbildeEgenvurderingDto
import no.nav.veilarbvedtaksstotte.utils.JsonUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
class PdfService(
    val pdfClient: PdfClient,
    val veilarbveilederClient: VeilarbveilederClient,
    val enhetInfoService: EnhetInfoService,
    val veilarbpersonClient: VeilarbpersonClient
) {
    val log = LoggerFactory.getLogger(PdfService::class.java)

    fun produserDokument(dto: ProduserDokumentDTO): ByteArray {

        val brevdataOppslag = hentBrevdata(dto.brukerFnr, dto.enhetId, dto.veilederIdent)
        val brevdata = DokumentService.mapBrevdata(dto, brevdataOppslag)

        return pdfClient.genererPdf(brevdata)
    }

    fun produserBehovsvurderingPdf(data: String?): Optional<ByteArray> {
        try {
            if (data != null) {

                val egenvurderingResponseDTO =
                    JsonUtils.objectMapper.readValue(data, OyeblikksbildeEgenvurderingDto::class.java);

                return Optional.ofNullable(
                    pdfClient.genererOyeblikksbildeEgenVurderingPdf(
                        egenvurderingResponseDTO
                    )
                )
            }
            return Optional.empty()
        } catch (e: Exception) {
            log.error("Kan ikke parse oyeblikksbilde data eller generere pdf", e);
            return Optional.empty()
        }
    }

    fun produserRegisteringPdf(data: String?): Optional<ByteArray> {
        try {
            if (data != null) {
                val registreringsdataResponseDto =
                    JsonUtils.objectMapper.readValue(data, RegistreringsdataDto::class.java);

                return Optional.ofNullable(
                    pdfClient.genererOyeblikksbildeRegistreringPdf(
                        registreringsdataResponseDto
                    )
                )
            }
            return Optional.empty()
        } catch (e: Exception) {
            log.error("Kan ikke parse oyeblikksbilde data eller generere pdf", e);
            return Optional.empty()
        }
    }

    fun produserCVPdf(data: String?): Optional<ByteArray> {
        try {
            if (data != null) {
                val cvDto =
                    JsonUtils.objectMapper.readValue(data, CvInnhold::class.java);
                return Optional.ofNullable(
                    pdfClient.genererOyeblikksbildeCvPdf(
                        cvDto
                    )
                )
            }
            return Optional.empty()
        } catch (e: Exception) {
            log.error("Kan ikke parse oyeblikksbilde data eller generere pdf", e);
            return Optional.empty()
        }
    }

    private fun hentBrevdata(fnr: Fnr, enhetId: EnhetId, veilederIdent: String): DokumentService.BrevdataOppslag {
        val enhetKontaktinformasjon: EnhetKontaktinformasjon = enhetInfoService.utledEnhetKontaktinformasjon(enhetId)
        val målform = veilarbpersonClient.hentMålform(fnr)
        val veilederNavn = veilarbveilederClient.hentVeilederNavn(veilederIdent)

        val enhet: Enhet = enhetInfoService.hentEnhet(enhetId)
        val kontaktEnhet: Enhet = enhetInfoService.hentEnhet(enhetKontaktinformasjon.enhetNr)

        return DokumentService.BrevdataOppslag(
            enhetKontaktinformasjon = enhetKontaktinformasjon,
            målform = målform,
            veilederNavn = veilederNavn,
            enhet = enhet,
            kontaktEnhet = kontaktEnhet
        )
    }
}