package no.nav.veilarbvedtaksstotte.klagebehandling.repository

import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.klagebehandling.domene.dto.OpprettKlageRequest
import no.nav.veilarbvedtaksstotte.utils.DatabaseTest
import no.nav.veilarbvedtaksstotte.utils.DbTestUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import java.time.LocalDate


class KlageRepositoryTest : DatabaseTest() {
    companion object {
        lateinit var klageRepository: KlageRepository

        @BeforeAll
        @JvmStatic
        fun setupOnce() {
            klageRepository = KlageRepository(jdbcTemplate)
        }
    }


    @BeforeEach
    fun setup() {
        DbTestUtils.cleanupDb(jdbcTemplate)
    }

    @Test
    fun `upsertKlageBakgrunnsdata skal opprette i databasen og kunne oppdatere felt`() {
        val vedtakId: Long = 123456789
        val norskIdent = "12345678910"
        val veilederIdent = "Z123456"
        val nyVeilederIdent = "Z654321"
        val request = OpprettKlageRequest(vedtakId, Fnr(norskIdent), veilederIdent)
        val oppdatertRequest = OpprettKlageRequest(vedtakId, Fnr(norskIdent), nyVeilederIdent)

        klageRepository.upsertKlageBakgrunnsdata(request)

        val lagretKlageUtenBrukerdata = klageRepository.hentKlageBehandling(vedtakId)
        assertNotNull(lagretKlageUtenBrukerdata)
        assertEquals(vedtakId, lagretKlageUtenBrukerdata.vedtakId)
        assertEquals(norskIdent, lagretKlageUtenBrukerdata.norskIdent)
        assertEquals(veilederIdent, lagretKlageUtenBrukerdata.veilederIdent)

        klageRepository.upsertKlageBakgrunnsdata(oppdatertRequest)

        val lagretKlage = klageRepository.hentKlageBehandling(vedtakId)
        assertNotNull(lagretKlage)
        assertEquals(nyVeilederIdent, lagretKlage.veilederIdent)

    }

    @Test
    fun `upsertKlageBrukerdata skal oppdatere felt for dato og begrunnelse`() {
        val vedtakId: Long = 123456789
        val klageDato = LocalDate.of(2026, 2, 14)
        val klageBegrunnelse = "Jeg er uenig i vedtaket fordi..."
        val nyKlageBegrunnelse = "Jeg har endret begrunnelsen"
        val defaultRequest = opprettEnDefaultKlage(vedtakId)
        klageRepository.upsertKlageBakgrunnsdata(defaultRequest)
        klageRepository.upsertKlageBrukerdata(vedtakId, klageDato, klageBegrunnelse)

        val lagretKlage = klageRepository.hentKlageBehandling(vedtakId)

        assertNotNull(lagretKlage)
        assertEquals(klageDato, lagretKlage.klageDato)
        assertEquals(klageBegrunnelse, lagretKlage.klageBegrunnelse)

        klageRepository.upsertKlageBrukerdata(vedtakId, klageDato, nyKlageBegrunnelse)
        val lagretOppdatertKlage = klageRepository.hentKlageBehandling(vedtakId)
        assertNotNull(lagretOppdatertKlage)
        assertEquals(nyKlageBegrunnelse, lagretOppdatertKlage.klageBegrunnelse)
    }


    private fun opprettEnDefaultKlage(vedtakId: Long): OpprettKlageRequest {
        val norskIdent = "12345678910"
        val veilederIdent = "Z123456"
        return OpprettKlageRequest(vedtakId, Fnr(norskIdent), veilederIdent)
    }

}
