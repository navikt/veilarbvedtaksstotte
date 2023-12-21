package no.nav.veilarbvedtaksstotte.service

import no.nav.veilarbvedtaksstotte.client.aiaBackend.AiaBackendClient
import no.nav.veilarbvedtaksstotte.client.aiaBackend.dto.Besvarelse
import no.nav.veilarbvedtaksstotte.client.aiaBackend.dto.EgenvurderingResponseDTO
import no.nav.veilarbvedtaksstotte.client.aiaBackend.dto.EgenvurderingResponseDTO.Tekster
import no.nav.veilarbvedtaksstotte.client.aiaBackend.dto.EndringIRegistreringsdataResponse
import no.nav.veilarbvedtaksstotte.client.person.VeilarbpersonClient
import no.nav.veilarbvedtaksstotte.client.registrering.dto.RegistreringResponseDto
import no.nav.veilarbvedtaksstotte.client.registrering.VeilarbregistreringClient
import no.nav.veilarbvedtaksstotte.repository.OyeblikksbildeRepository
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository
import no.nav.veilarbvedtaksstotte.utils.JsonUtils.fromJson
import no.nav.veilarbvedtaksstotte.utils.toJson
import org.joda.time.Instant
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.LocalDateTime

internal class OyeblikksbildeServiceTest {
    @Test
    fun mapToEgenvurderingDataJson() {
        val egenvurderingstekster: MutableMap<String, String> = HashMap()
        egenvurderingstekster["SITUASJONSBESTEMT_INNSATS"] = "Jeg vil få hjelp fra NAV"
        val egenvurderingDato = Instant().toString()
        val egenvurdering = EgenvurderingResponseDTO(
            egenvurderingDato,
            "dialog-123",
            "SITUASJONSBESTEMT_INNSATS",
            EgenvurderingResponseDTO.Tekster("Ønsker du veiledning?", egenvurderingstekster)
        )
        val egenvurderingJson = oyeblikksbildeService.mapToEgenvurderingData(egenvurdering).toJson()
        val forventetEgenvurderingJson =
            "{\"sistOppdatert\":\"$egenvurderingDato\",\"svar\":[{\"spm\":\"Ønsker du veiledning?\",\"svar\":\"Jeg vil få hjelp fra NAV\",\"oppfolging\":\"SITUASJONSBESTEMT_INNSATS\",\"dialogId\":\"dialog-123\"}]}"
        Assertions.assertEquals(forventetEgenvurderingJson, egenvurderingJson)
    }

    @Test
    fun mapToEgenvurderingDataJson_med_null_argument() {
        val oyeblikksbildeEgenvurderingDto = oyeblikksbildeService.mapToEgenvurderingData(null)
        Assertions.assertNull(oyeblikksbildeEgenvurderingDto.svar)
        Assertions.assertNull(oyeblikksbildeEgenvurderingDto.sistOppdatert)
    }

    @Test
    fun mapToEgenvurderingDataJson_ikke_alle_verdier_satt() {
        val egenvurderingstekster: MutableMap<String, String> = HashMap()
        egenvurderingstekster["SITUASJONSBESTEMT_INNSATS"] = "Jeg vil få hjelp fra NAV"
        val egenvurdering = EgenvurderingResponseDTO(
            null,
            null,
            "SITUASJONSBESTEMT_INNSATS",
            EgenvurderingResponseDTO.Tekster("Ønsker du veiledning?", egenvurderingstekster)
        )
        val egenvurderingJson = oyeblikksbildeService.mapToEgenvurderingData(egenvurdering).toJson()
        val forventetEgenvurderingJson =
            "{\"sistOppdatert\":null,\"svar\":[{\"spm\":\"Ønsker du veiledning?\",\"svar\":\"Jeg vil få hjelp fra NAV\",\"oppfolging\":\"SITUASJONSBESTEMT_INNSATS\",\"dialogId\":null}]}"
        Assertions.assertEquals(forventetEgenvurderingJson, egenvurderingJson)
    }

    @Test
    fun oppdaterRegistreringsdataHvisNyeEndringer() {
        val registreringsDataJson =
            "{\"registrering\":{\"id\":10004240,\"opprettetDato\":\"2023-06-22T16:47:18.325956+02:00\",\"besvarelse\":{\"utdanning\":\"HOYERE_UTDANNING_5_ELLER_MER\",\"utdanningBestatt\":\"JA\",\"utdanningGodkjent\":\"JA\",\"helseHinder\":\"NEI\",\"andreForhold\":\"NEI\"," +
                    "\"sisteStilling\":\"INGEN_SVAR\",\"dinSituasjon\":\"MISTET_JOBBEN\",\"fremtidigSituasjon\":null,\"tilbakeIArbeid\":null},\"teksterForBesvarelse\":[{\"sporsmalId\":\"dinSituasjon\",\"sporsmal\":\"Velg den situasjonen som passer deg best\",\"svar\":\"Har mistet eller kommer til å miste jobben\"},{\"sporsmalId\":\"utdanning\",\"sporsmal\":\"Hva er din høyeste fullførte utdanning?\"," +
                    "\"svar\":\"Høyere utdanning (5 år eller mer)\"},{" +
                    "\"sporsmalId\":\"utdanningGodkjent\",\"sporsmal\":\"Er utdanningen din godkjent i Norge?\",\"svar\":\"Ja\"},{\"sporsmalId\":\"utdanningBestatt\",\"sporsmal\":\"Er utdanningen din bestått?\",\"svar\":\"Ja\"},{\"sporsmalId\":\"andreForhold\",\"sporsmal\":\"Har du andre problemer med å søke eller være i jobb?\",\"svar\":\"Nei\"},{" +
                    "\"sporsmalId\":\"sisteStilling\",\"sporsmal\":\"Hva er din siste jobb?\",\"svar\":\"Annen stilling\"},{" +
                    "\"sporsmalId\":\"helseHinder\",\"sporsmal\":\"Har du helseproblemer som hindrer deg i å søke eller være i jobb?\",\"svar\":\"Nei\"}]," +
                    "\"sisteStilling\": {\"label\":\"Annen stilling\",\"konseptId\": -1,\"styrk08\":\"-1\"}," +
                    "\"profilering\": {\"innsatsgruppe\":\"SITUASJONSBESTEMT_INNSATS\",\"alder\": 28,\"jobbetSammenhengendeSeksAvTolvSisteManeder\": false}," +
                    "\"manueltRegistrertAv\": null},\"type\":\"ORDINAER\"}"

        val registreringsDataDto = fromJson(registreringsDataJson, RegistreringResponseDto::class.java)

        val endringIRegistreringsdata = EndringIRegistreringsdataResponse(
            registreringsId = 10004400,
            endretAv = "BRUKER",
            endretTidspunkt = LocalDateTime.parse("2023-07-18T11:24:03.158629"),
            registreringsTidspunkt = LocalDateTime.parse("2023-07-17T11:27:25.299658"),
            opprettetAv = "BRUKER",
            erBesvarelsenEndret = true,
            besvarelse = Besvarelse(
                utdanning = Besvarelse.Utdanning(
                    verdi = "HOYERE_UTDANNING_1_TIL_4",
                    gjelderFraDato = null,
                    gjelderTilDato = null,
                    endretAv = null,
                    endretTidspunkt = null
                ),
                utdanningBestatt = Besvarelse.UtdanningBestatt(
                    verdi = "JA",
                    gjelderFraDato = null,
                    gjelderTilDato = null,
                    endretAv = null,
                    endretTidspunkt = null
                ),
                utdanningGodkjent = Besvarelse.UtdanningGodkjent(
                    verdi = "JA",
                    gjelderFraDato = null,
                    gjelderTilDato = null,
                    endretAv = null,
                    endretTidspunkt = null
                ),
                helseHinder = Besvarelse.HelseHinder(
                    verdi = "JA",
                    gjelderFraDato = null,
                    gjelderTilDato = null,
                    endretAv = null,
                    endretTidspunkt = null
                ),
                andreForhold = Besvarelse.AndreForhold(
                    verdi = "NEI",
                    gjelderFraDato = null,
                    gjelderTilDato = null,
                    endretAv = null,
                    endretTidspunkt = null
                ),
                sisteStilling = Besvarelse.SisteStilling(
                    verdi = "INGEN_SVAR",
                    gjelderFraDato = null,
                    gjelderTilDato = null,
                    endretAv = null,
                    endretTidspunkt = null
                ),
                fremtidigSituasjon = Besvarelse.FremtidigSituasjon(
                    verdi = null,
                    gjelderFraDato = null,
                    gjelderTilDato = null,
                    endretAv = null,
                    endretTidspunkt = null
                ),
                tilbakeIArbeid = Besvarelse.TilbakeIArbeid(
                    verdi = null,
                    gjelderFraDato = null,
                    gjelderTilDato = null,
                    endretAv = null,
                    endretTidspunkt = null
                ),
                dinSituasjon = Besvarelse.DinSituasjon(
                    verdi = "OPPSIGELSE",
                    tilleggsData = Besvarelse.DinSituasjon.TilleggsData(
                        forsteArbeidsdagDato = null,
                        sisteArbeidsdagDato = "2023-07-31",
                        oppsigelseDato = "2023-07-19",
                        gjelderFraDato = null,
                        permitteringsProsent = null,
                        stillingsProsent = null,
                        permitteringForlenget = null,
                        harNyJobb = null
                    ),
                    gjelderFraDato = null,
                    gjelderTilDato = null,
                    endretAv = "BRUKER",
                    endretTidspunkt = "2023-07-18T11:24:03.136693338"
                )
            )
        )

        val expectedData =
            "{\"registrering\":{\"id\":10004240,\"opprettetDato\":\"2023-06-22T16:47:18.325956+02:00\",\"besvarelse\":{\"utdanning\":\"HOYERE_UTDANNING_5_ELLER_MER\",\"utdanningBestatt\":\"JA\",\"utdanningGodkjent\":\"JA\",\"helseHinder\":\"NEI\",\"andreForhold\":\"NEI\"," +
                    "\"sisteStilling\":\"INGEN_SVAR\",\"dinSituasjon\":\"OPPSIGELSE\",\"fremtidigSituasjon\":null,\"tilbakeIArbeid\":null},\"teksterForBesvarelse\":[{\"sporsmalId\":\"dinSituasjon\",\"sporsmal\":\"Velg den situasjonen som passer deg best\",\"svar\":\"Jeg har blitt sagt opp av arbeidsgiver\"},{\"sporsmalId\":\"utdanning\",\"sporsmal\":\"Hva er din høyeste fullførte utdanning?\"," +
                    "\"svar\":\"Høyere utdanning (5 år eller mer)\"},{" +
                    "\"sporsmalId\":\"utdanningGodkjent\",\"sporsmal\":\"Er utdanningen din godkjent i Norge?\",\"svar\":\"Ja\"},{\"sporsmalId\":\"utdanningBestatt\",\"sporsmal\":\"Er utdanningen din bestått?\",\"svar\":\"Ja\"},{\"sporsmalId\":\"andreForhold\",\"sporsmal\":\"Har du andre problemer med å søke eller være i jobb?\",\"svar\":\"Nei\"},{" +
                    "\"sporsmalId\":\"sisteStilling\",\"sporsmal\":\"Hva er din siste jobb?\",\"svar\":\"Annen stilling\"},{" +
                    "\"sporsmalId\":\"helseHinder\",\"sporsmal\":\"Har du helseproblemer som hindrer deg i å søke eller være i jobb?\",\"svar\":\"Nei\"}]," +
                    "\"sisteStilling\": {\"label\":\"Annen stilling\",\"konseptId\": -1,\"styrk08\":\"-1\"}," +
                    "\"profilering\": {\"innsatsgruppe\":\"SITUASJONSBESTEMT_INNSATS\",\"alder\": 28,\"jobbetSammenhengendeSeksAvTolvSisteManeder\": false}," +
                    "\"manueltRegistrertAv\": null, \"endretAv\":\"BRUKER\", \"endretTidspunkt\":\"2023-07-18T11:24:03.158629\"},\"type\":\"ORDINAER\"}"

        val expectedOppdatertRegistreringData = fromJson(expectedData, RegistreringResponseDto::class.java)

        val actualData = oyeblikksbildeService.oppdaterRegistreringsdataHvisNyeEndringer(
            registreringsDataDto,
            endringIRegistreringsdata
        )
        Assertions.assertEquals(expectedOppdatertRegistreringData, actualData)

    }

    companion object {
        private val authService = Mockito.mock(AuthService::class.java)
        private val oyeblikksbildeRepository = Mockito.mock(
            OyeblikksbildeRepository::class.java
        )
        private val vedtaksstotteRepository = Mockito.mock(
            VedtaksstotteRepository::class.java
        )
        private val veilarbpersonClient = Mockito.mock(VeilarbpersonClient::class.java)
        private val registreringClient = Mockito.mock(
            VeilarbregistreringClient::class.java
        )
        private val aiaBackendClient = Mockito.mock(AiaBackendClient::class.java)
        private val oyeblikksbildeService = OyeblikksbildeService(
            authService,
            oyeblikksbildeRepository,
            vedtaksstotteRepository,
            veilarbpersonClient,
            registreringClient,
            aiaBackendClient
        )
    }
}
