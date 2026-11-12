package no.nav.veilarbvedtaksstotte.service

import no.nav.common.types.identer.NorskIdent
import no.nav.veilarbvedtaksstotte.client.veilarbportefolje.VeilarbportefoljeClient
import no.nav.veilarbvedtaksstotte.client.veilarbportefolje.dto.Etiketter
import no.nav.veilarbvedtaksstotte.client.veilarbportefolje.dto.EtiketterForBrukerDTO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

class EtiketterServiceTest {

    private val veilarbportefoljeClient: VeilarbportefoljeClient = mock(VeilarbportefoljeClient::class.java)
    private val etiketterService = EtiketterService(veilarbportefoljeClient)

    @Test
    fun `hentEtiketter mapper etiketter til fnr`() {
        val etiketter = Etiketter(erDoed = false, egenAnsatt = true, adressebeskyttelse = null, kandidatForUtmelding = false)
        whenever(veilarbportefoljeClient.hentEtiketter(listOf(NorskIdent.of("11111111111"))))
            .thenReturn(listOf(EtiketterForBrukerDTO(NorskIdent.of("11111111111"), etiketter)))

        val resultat = etiketterService.hentEtiketter(listOf("11111111111", "", "11111111111"))

        assertEquals(mapOf("11111111111" to etiketter), resultat)
    }

    @Test
    fun `hentEtiketter kaller ikke klient når det ikke finnes fnr`() {
        assertEquals(emptyMap<String, Etiketter>(), etiketterService.hentEtiketter(listOf("")))
        verifyNoInteractions(veilarbportefoljeClient)
    }

    @Test
    fun `hentEtiketter returnerer tomt map når klienten feiler`() {
        whenever(veilarbportefoljeClient.hentEtiketter(any())).thenThrow(RuntimeException("feil"))

        assertEquals(emptyMap<String, Etiketter>(), etiketterService.hentEtiketter(listOf("11111111111")))
    }
}
