package no.nav.veilarbvedtaksstotte.service

import io.getunleash.DefaultUnleash
import no.nav.veilarbvedtaksstotte.client.aiaBackend.AiaBackendClient
import no.nav.veilarbvedtaksstotte.client.aiaBackend.dto.EgenvurderingResponseDTO
import no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.ArbeidssoekerregisteretApiOppslagV2Client
import no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.EgenvurderingDialogTjenesteClient
import no.nav.veilarbvedtaksstotte.client.person.VeilarbpersonClient
import no.nav.veilarbvedtaksstotte.domain.vedtak.KildeEntity
import no.nav.veilarbvedtaksstotte.repository.OyeblikksbildeRepository
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository
import no.nav.veilarbvedtaksstotte.utils.toJson
import org.joda.time.Instant
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.util.*

internal class OyeblikksbildeServiceTest {

    @Test
    fun lagreOyeblikksbilderPaaNynorsk() {
        val fnr = "12345678910"
        val egenvurderingTekst = "Svara dine om behov for rettleiing"
        val arbeissøkerregisteretTekst = "Det du fortalde oss da du vart registrert som arbeidssøkar"
        val kilder = listOf(
            KildeEntity(egenvurderingTekst, UUID.randomUUID()),
            KildeEntity(arbeissøkerregisteretTekst, UUID.randomUUID())
        )
        oyeblikksbildeService.lagreOyeblikksbilde(fnr, 12344, kilder)
        Mockito.verify(oyeblikksbildeRepository, Mockito.times(1))
            .upsertArbeidssokerRegistretOyeblikksbilde(12344, null)
        Mockito.verify(oyeblikksbildeRepository, Mockito.times(1)).upsertEgenvurderingOyeblikksbilde(12344, null)
    }

    @Test
    fun mapToEgenvurderingDataJson() {
        val egenvurderingstekster: MutableMap<String, String> = HashMap()
        egenvurderingstekster["SITUASJONSBESTEMT_INNSATS"] = "Jeg vil få hjelp fra Nav"
        val egenvurderingDato = Instant().toString()
        val egenvurdering = EgenvurderingResponseDTO(
            egenvurderingDato,
            "dialog-123",
            "SITUASJONSBESTEMT_INNSATS",
            EgenvurderingResponseDTO.Tekster("Ønsker du veiledning?", egenvurderingstekster)
        )
        val egenvurderingJson = OyeblikksbildeService.mapToEgenvurderingData(egenvurdering).toJson()
        val forventetEgenvurderingJson =
            "{\"sistOppdatert\":\"$egenvurderingDato\",\"svar\":[{\"spm\":\"Ønsker du veiledning?\",\"svar\":\"Jeg vil få hjelp fra Nav\",\"oppfolging\":\"SITUASJONSBESTEMT_INNSATS\",\"dialogId\":\"dialog-123\"}]}"
        Assertions.assertEquals(forventetEgenvurderingJson, egenvurderingJson)
    }

    @Test
    fun mapToEgenvurderingDataJson_med_null_argument() {
        val oyeblikksbildeEgenvurderingDto = OyeblikksbildeService.mapToEgenvurderingData(null)
        Assertions.assertNull(oyeblikksbildeEgenvurderingDto)
    }

    @Test
    fun mapToEgenvurderingDataJson_ikke_alle_verdier_satt() {
        val egenvurderingstekster: MutableMap<String, String> = HashMap()
        egenvurderingstekster["SITUASJONSBESTEMT_INNSATS"] = "Jeg vil få hjelp fra Nav"
        val egenvurdering = EgenvurderingResponseDTO(
            null,
            null,
            "SITUASJONSBESTEMT_INNSATS",
            EgenvurderingResponseDTO.Tekster("Ønsker du veiledning?", egenvurderingstekster)
        )
        val egenvurderingJson = OyeblikksbildeService.mapToEgenvurderingData(egenvurdering).toJson()
        val forventetEgenvurderingJson =
            "{\"sistOppdatert\":null,\"svar\":[{\"spm\":\"Ønsker du veiledning?\",\"svar\":\"Jeg vil få hjelp fra Nav\",\"oppfolging\":\"SITUASJONSBESTEMT_INNSATS\",\"dialogId\":null}]}"
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

        private val aiaBackendClient = Mockito.mock(AiaBackendClient::class.java)
        private val arbeidssoekerregisteretApiOppslagV2Client =
            Mockito.mock(ArbeidssoekerregisteretApiOppslagV2Client::class.java)
        private val egenvurderingDialogTjenesteClient = Mockito.mock(EgenvurderingDialogTjenesteClient::class.java)
        private val defaultUnleash = Mockito.mock(DefaultUnleash::class.java)
        private val oyeblikksbildeService = OyeblikksbildeService(
            authService,
            oyeblikksbildeRepository,
            vedtaksstotteRepository,
            veilarbpersonClient,
            aiaBackendClient,
            arbeidssoekerregisteretApiOppslagV2Client,
            egenvurderingDialogTjenesteClient,
            defaultUnleash
        )
    }
}
