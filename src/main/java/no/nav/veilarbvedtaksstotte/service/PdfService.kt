package no.nav.veilarbvedtaksstotte.service

import io.getunleash.DefaultUnleash
import io.getunleash.UnleashContext
import no.nav.common.client.norg2.Enhet
import no.nav.common.types.identer.EnhetId
import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.client.person.OpplysningerOmArbeidssoekerMedProfilering
import no.nav.veilarbvedtaksstotte.client.dokument.ProduserDokumentDTO
import no.nav.veilarbvedtaksstotte.client.norg2.EnhetKontaktinformasjon
import no.nav.veilarbvedtaksstotte.client.pdf.*
import no.nav.veilarbvedtaksstotte.client.person.VeilarbpersonClient
import no.nav.veilarbvedtaksstotte.client.person.dto.CvInnhold
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.VeilarbveilederClient
import no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.EgenvurderingDto
import no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.EgenvurderingV2Dto
import no.nav.veilarbvedtaksstotte.utils.JsonUtils
import no.nav.veilarbvedtaksstotte.utils.SKJULE_VEILEDERS_NAVN_14A_VEDTAKSBREV
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
class PdfService(
    val pdfClient: PdfClient,
    val veilarbveilederClient: VeilarbveilederClient,
    val enhetInfoService: EnhetInfoService,
    val veilarbpersonClient: VeilarbpersonClient,
    val unleashService: DefaultUnleash,
) {
    val log = LoggerFactory.getLogger(PdfService::class.java)

    fun produserDokument(dto: ProduserDokumentDTO): ByteArray {
        val brevdataOppslag = hentBrevdata(dto.brukerFnr, dto.enhetId, dto.veilederIdent)
        val vasketDto = vaskVedtakDto(dto)

        val unleashContext = UnleashContext.builder()
            .userId(dto.veilederIdent)
            .build()

        val brevdataOppslagUtenNavn =
            if (unleashService.isEnabled(SKJULE_VEILEDERS_NAVN_14A_VEDTAKSBREV, unleashContext)) {
                // Hvis funksjonen er skrudd på, skal veilederNavn være null
                log.info("Funksjon for å skjule veileders navn i 14A vedtaksbrev er aktivert.")

                brevdataOppslag.copy(veilederNavn = "")
            } else {
                brevdataOppslag
            }

        val brevdataDto = DokumentService.mapBrevdata(vasketDto, brevdataOppslagUtenNavn)

        return pdfClient.genererPdf(brevdataDto)
    }

    fun produserBehovsvurderingPdf(data: String?, mottaker: Mottaker): Optional<ByteArray> {
        try {
            if (data == null) return Optional.empty()

            val egenvurderingResponseDTO =
                JsonUtils.objectMapper.readValue(data, EgenvurderingDto::class.java)

            val egenvurderingMedMottaker = EgenvurderingMedMottakerDto.from(egenvurderingResponseDTO, mottaker)

            return Optional.ofNullable(
                pdfClient.genererOyeblikksbildeEgenVurderingPdf(
                    egenvurderingMedMottaker
                )
            )
        } catch (e: Exception) {
            log.error("Kan ikke parse oyeblikksbilde data eller generere pdf", e)
            throw e
        }
    }

    fun produserEgenvurderingV2Pdf(data: String?, mottaker: Mottaker): Optional<ByteArray> {
        try {
            if (data == null) return Optional.empty()

            val egenvurderingV2Dto =
                JsonUtils.objectMapper.readValue(data, EgenvurderingV2Dto::class.java)

            val egenvurderingMedMottaker = EgenvurderingMedMottakerDto.from(egenvurderingV2Dto, mottaker)

            return Optional.ofNullable(
                pdfClient.genererOyeblikksbildeEgenVurderingPdf(
                    egenvurderingMedMottaker
                )
            )
        } catch (e: Exception) {
            log.error("Kan ikke parse oyeblikksbilde data eller generere pdf", e)
            throw e
        }
    }

    fun produserArbeidssokerRegistretPdf(data: String?, mottaker: Mottaker): Optional<ByteArray> {
        try {
            if (data == null) return Optional.empty()

            val registreringsdataResponseDto =
                JsonUtils.objectMapper.readValue(data, OpplysningerOmArbeidssoekerMedProfilering::class.java)

            val registreringsdataMedMottaker =
                OpplysningerOmArbeidssoekerMedProfileringMedMottakerDto.from(registreringsdataResponseDto, mottaker)

            return Optional.ofNullable(
                pdfClient.genererOyeblikksbildeArbeidssokerRegistretPdf(
                    registreringsdataMedMottaker
                )
            )
        } catch (e: Exception) {
            log.error("Kan ikke parse oyeblikksbilde data eller generere pdf", e)
            throw e
        }
    }

    fun produserCVPdf(data: String?, mottaker: Mottaker): Optional<ByteArray> {
        try {
            if (data == null) return Optional.empty()

            val cvDto = JsonUtils.objectMapper.readValue(data, CvInnhold::class.java)
            val cvInnholdMedMottaker = CvInnholdMedMottakerDto.from(cvDto, mottaker)

            return Optional.ofNullable(
                pdfClient.genererOyeblikksbildeCvPdf(
                    cvInnholdMedMottaker
                )
            )
        } catch (e: Exception) {
            log.error("Kan ikke parse oyeblikksbilde data eller generere pdf", e)
            throw e
        }
    }

    private fun hentBrevdata(fnr: Fnr, enhetId: EnhetId, veilederIdent: String): DokumentService.BrevdataOppslag {
        val enhetKontaktinformasjon: EnhetKontaktinformasjon = enhetInfoService.utledEnhetKontaktinformasjon(enhetId)
        val malform = veilarbpersonClient.hentMalform(fnr)
        val veilederNavn = veilarbveilederClient.hentVeilederNavn(veilederIdent)
        val fodselsdatoOgAr = veilarbpersonClient.hentFodselsdato(fnr)

        val enhet: Enhet = enhetInfoService.hentEnhet(enhetId)
        val kontaktEnhet: Enhet = enhetInfoService.hentEnhet(enhetKontaktinformasjon.enhetNr)

        return DokumentService.BrevdataOppslag(
            enhetKontaktinformasjon = enhetKontaktinformasjon,
            malform = malform,
            veilederNavn = veilederNavn,
            enhet = enhet,
            kontaktEnhet = kontaktEnhet,
            fodselsdatoOgAr = fodselsdatoOgAr
        )
    }

    fun vaskVedtakDto(dto: ProduserDokumentDTO): ProduserDokumentDTO {
        return dto.copy(begrunnelse = dto.begrunnelse?.let { vaskStringForUgyldigeTegn(it) } ?: "")
    }

}
