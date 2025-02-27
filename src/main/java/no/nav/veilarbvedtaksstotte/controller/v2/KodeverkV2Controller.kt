package no.nav.veilarbvedtaksstotte.controller.v2

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.veilarbvedtaksstotte.annotations.EksterntEndepunkt
import no.nav.veilarbvedtaksstotte.controller.v2.dto.HovedmalKodeverkDTO
import no.nav.veilarbvedtaksstotte.controller.v2.dto.InnsatsgruppeKodeverkDTO
import no.nav.veilarbvedtaksstotte.controller.v2.dto.KodeverkV2DTO
import no.nav.veilarbvedtaksstotte.domain.vedtak.HovedmalDetaljertV2
import no.nav.veilarbvedtaksstotte.domain.vedtak.InnsatsgruppeDetaljertV2
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/open/api/v2/kodeverk")
@Tag(
    name = "Kodeverk for § 14 a-vedtak",
    description = "Funksjonalitet knyttet til kodeverk for data fra § 14 a-vedtak (innsatsgruppe og hovedmål)."
)
class KodeverkV2Controller {
    private val kodeverk = lagKodeverk()

    @EksterntEndepunkt
    @GetMapping("/innsatsgruppe")
    @Operation(
        summary = "Hent kodeverk for innsatsgruppe",
        description = "Henter kodeverket for innsatsgrupper. En innsatsgruppe representerer konklusjonen i et § 14 a-vedtak. Med konklusjon menes hvilken/hvor mye arbeidsrettet oppfølging en bruker har rett på.",
        responses = [ApiResponse(
            responseCode = "200",
            content = [Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                array = ArraySchema(schema = Schema(implementation = InnsatsgruppeKodeverkDTO::class))
            )]
        )]
    )
    fun getInnsatsgrupper(): List<InnsatsgruppeKodeverkDTO> {
        return kodeverk.innsatsgrupper
    }

    @EksterntEndepunkt
    @GetMapping("/hovedmal")
    @Operation(
        summary = "Hent kodeverk for hovedmål",
        description = "Henter kodeverket for hovedmål. Et hovedmål representerer brukers mål med den arbeidsrettede oppfølgingen og er en del av begrunnelsen i et § 14 a-vedtak.",
        responses = [ApiResponse(
            responseCode = "200",
            content = [Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                array = ArraySchema(schema = Schema(implementation = HovedmalKodeverkDTO::class))
            )]
        )]
    )
    fun getHovedmal(): List<HovedmalKodeverkDTO> {
        return kodeverk.hovedmal
    }

    @EksterntEndepunkt
    @GetMapping("/innsatsgruppeoghovedmal")
    @Operation(
        summary = "Hent kodeverk for innsatsgruppe og hovedmål",
        description = """
    Henter kodeverket for både innsatsgruppe og hovedmål.

    En innsatsgruppe representerer konklusjonen i et § 14 a-vedtak. Med konklusjon menes hvilken/hvor mye arbeidsrettet oppfølging en bruker har rett på.
    Et hovedmål representerer brukers mål med den arbeidsrettede oppfølgingen og er en del av begrunnelsen i et § 14 a-vedtak.""",
        responses = [ApiResponse(
            responseCode = "200",
            content = [Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = Schema(implementation = KodeverkV2DTO::class)
            )]
        )]
    )
    fun getInnsatsgruppeOgHovedmalKodeverk(): KodeverkV2DTO {
        return kodeverk
    }

    private fun lagKodeverk(): KodeverkV2DTO {
        return KodeverkV2DTO(
            innsatsgrupper = InnsatsgruppeDetaljertV2.entries.map {
                InnsatsgruppeKodeverkDTO(
                    kode = it.kode,
                    gammelKode = it.gammelKode,
                    arenaKode = it.arenaKode,
                    beskrivelse = it.beskrivelse,
                )
            },
            hovedmal = HovedmalDetaljertV2.entries.map {
                HovedmalKodeverkDTO(
                    kode = it.kode,
                    beskrivelse = it.beskrivelse
                )
            }
        )
    }
}
