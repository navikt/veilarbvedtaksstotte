package no.nav.veilarbvedtaksstotte.service

import no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.ArbeidssoekerregisteretApiOppslagV2Client
import no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.EgenvurderingDialogTjenesteClient
import no.nav.veilarbvedtaksstotte.client.person.VeilarbpersonClient
import no.nav.veilarbvedtaksstotte.domain.vedtak.KildeEntity
import no.nav.veilarbvedtaksstotte.repository.OyeblikksbildeRepository
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository
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

        private val arbeidssoekerregisteretApiOppslagV2Client =
            Mockito.mock(ArbeidssoekerregisteretApiOppslagV2Client::class.java)
        private val egenvurderingDialogTjenesteClient = Mockito.mock(EgenvurderingDialogTjenesteClient::class.java)
        private val oyeblikksbildeService = OyeblikksbildeService(
            authService,
            oyeblikksbildeRepository,
            vedtaksstotteRepository,
            veilarbpersonClient,
            arbeidssoekerregisteretApiOppslagV2Client,
            egenvurderingDialogTjenesteClient
        )
    }
}
