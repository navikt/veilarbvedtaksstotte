package no.nav.veilarbvedtaksstotte.service

import no.nav.veilarbvedtaksstotte.client.aiaBackend.AiaBackendClient
import no.nav.veilarbvedtaksstotte.client.aiaBackend.dto.EgenvurderingResponseDTO
import no.nav.veilarbvedtaksstotte.client.arbeidssoekeregisteret.ArbeidssoekerRegisteretService
import no.nav.veilarbvedtaksstotte.client.person.VeilarbpersonClient
import no.nav.veilarbvedtaksstotte.repository.OyeblikksbildeRepository
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository
import no.nav.veilarbvedtaksstotte.utils.toJson
import org.joda.time.Instant
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito

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
        Assertions.assertNull(oyeblikksbildeEgenvurderingDto)
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


    companion object {
        private val authService = Mockito.mock(AuthService::class.java)
        private val oyeblikksbildeRepository = Mockito.mock(
            OyeblikksbildeRepository::class.java
        )
        private val vedtaksstotteRepository = Mockito.mock(
            VedtaksstotteRepository::class.java
        )
        private val veilarbpersonClient = Mockito.mock(VeilarbpersonClient::class.java)
        private val arbeidssoekerRegisteretService = Mockito.mock(ArbeidssoekerRegisteretService::class.java)

        private val aiaBackendClient = Mockito.mock(AiaBackendClient::class.java)
        private val oyeblikksbildeService = OyeblikksbildeService(
            authService,
            oyeblikksbildeRepository,
            vedtaksstotteRepository,
            veilarbpersonClient,
            aiaBackendClient,
            arbeidssoekerRegisteretService
        )
    }
}
