package no.nav.veilarbvedtaksstotte.klagebehandling.repository

import no.nav.veilarbvedtaksstotte.utils.DatabaseTest
import no.nav.veilarbvedtaksstotte.utils.DbTestUtils
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
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
    fun `lagre ny klagebehandling med initiell bakgrunnsdata`() {
        val vedtakId: Long = 123456789
        val norskIdent = "12345678910"
        val veilederIdent = "Z123456"
        val klageDato = LocalDate.of(2026, 2, 14)
        val klageBegrunnelse = "Jeg er uenig i vedtaket fordi..."

        assertDoesNotThrow {
            klageRepository.upsertKlageBakgrunnsdata(
                vedtakId,
                norskIdent,
                veilederIdent,
                klageDato,
                klageBegrunnelse
            )
        }

        val lagretKlage = klageRepository.hentKlageBehandling(vedtakId)
        assertNotNull(lagretKlage)
        assertEquals(vedtakId, lagretKlage.vedtakId)
        assertEquals(norskIdent, lagretKlage.norskIdent)
        assertEquals(veilederIdent, lagretKlage.veilederIdent)
        assertEquals(klageDato, lagretKlage.klageDato)
        assertEquals(klageBegrunnelse, lagretKlage.klageBegrunnelse)

    }

    @Test
    fun `upsert klagebehandling skal oppdatere felt`() {
        val vedtakId: Long = 123456789
        val norskIdent = "12345678910"
        val veilederIdent = "Z123456"
        val klageDato = LocalDate.of(2026, 2, 14)
        val klageBegrunnelse = "Jeg er uenig i vedtaket fordi..."

        klageRepository.upsertKlageBakgrunnsdata(
            vedtakId,
            norskIdent,
            veilederIdent,
            null,
            null
        )

        val lagretKlageUtenBrukerdata = klageRepository.hentKlageBehandling(vedtakId)
        assertNotNull(lagretKlageUtenBrukerdata)
        assertEquals(vedtakId, lagretKlageUtenBrukerdata.vedtakId)
        assertEquals(norskIdent, lagretKlageUtenBrukerdata.norskIdent)
        assertEquals(veilederIdent, lagretKlageUtenBrukerdata.veilederIdent)
        assertNull(lagretKlageUtenBrukerdata.klageDato)
        assertNull(lagretKlageUtenBrukerdata.klageBegrunnelse)

        klageRepository.upsertKlageBakgrunnsdata(
            vedtakId,
            norskIdent,
            veilederIdent,
            klageDato,
            klageBegrunnelse
        )

        val lagretKlageMedBrukerdata = klageRepository.hentKlageBehandling(vedtakId)
        assertNotNull(lagretKlageMedBrukerdata)
        assertEquals(klageDato, lagretKlageMedBrukerdata.klageDato)
        assertEquals(klageBegrunnelse, lagretKlageMedBrukerdata.klageBegrunnelse)

    }


}
