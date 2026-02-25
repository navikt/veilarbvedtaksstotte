package no.nav.veilarbvedtaksstotte.klagebehandling.repository

import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.klagebehandling.domene.FormkravOppfylt
import no.nav.veilarbvedtaksstotte.klagebehandling.domene.dto.OpprettKlageRequest
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
    fun `upsertOpprettKlagebehandling skal opprette i databasen og kunne oppdatere felt`() {
        val vedtakId: Long = 123456789
        val norskIdent = "12345678910"
        val veilederIdent = "Z123456"
        val nyVeilederIdent = "Z654321"
        val request = OpprettKlageRequest(vedtakId, Fnr(norskIdent), veilederIdent)
        val oppdatertRequest = OpprettKlageRequest(vedtakId, Fnr(norskIdent), nyVeilederIdent)

        klageRepository.upsertOpprettKlagebehandling(request)

        val lagretKlageUtenBrukerdata = klageRepository.hentKlageBehandling(vedtakId)
        assertNotNull(lagretKlageUtenBrukerdata)
        assertEquals(vedtakId, lagretKlageUtenBrukerdata.vedtakId)
        assertEquals(norskIdent, lagretKlageUtenBrukerdata.norskIdent)
        assertEquals(veilederIdent, lagretKlageUtenBrukerdata.veilederIdent)

        klageRepository.upsertOpprettKlagebehandling(oppdatertRequest)

        val lagretKlage = klageRepository.hentKlageBehandling(vedtakId)
        assertNotNull(lagretKlage)
        assertEquals(nyVeilederIdent, lagretKlage.veilederIdent)
    }

    @Test
    fun `upsertKlageBrukerdata skal oppdatere felt for dato og journalpostId`() {
        val vedtakId: Long = 123456789
        val klageDato = LocalDate.of(2026, 2, 14)
        val journalpostId = "987654321"

        val defaultRequest = opprettEnDefaultKlage(vedtakId)
        klageRepository.upsertOpprettKlagebehandling(defaultRequest)
        klageRepository.upsertKlageBrukerdata(vedtakId, klageDato, journalpostId)

        val lagretKlage = klageRepository.hentKlageBehandling(vedtakId)

        assertNotNull(lagretKlage)
        assertEquals(klageDato, lagretKlage.klageDato)
        assertEquals(journalpostId, lagretKlage.klageJournalpostid)
    }

    @Test
    fun `upsertFormkrav skal oppdatere felt for formkravOppfylt og formkravBegrunnelse`() {
        val vedtakId: Long = 123456789
        val formkravBegrunnelse = "Alle formkrav er oppfylt."

        val defaultRequest = opprettEnDefaultKlage(vedtakId)
        klageRepository.upsertOpprettKlagebehandling(defaultRequest)
        klageRepository.upsertFormkrav(vedtakId, FormkravOppfylt.OPPFYLT, null)

        val lagretKlageOppfylt = klageRepository.hentKlageBehandling(vedtakId)
        assertNotNull(lagretKlageOppfylt)
        assertEquals(FormkravOppfylt.OPPFYLT, lagretKlageOppfylt.formkravOppfylt)
        assertNull(lagretKlageOppfylt.formkravBegrunnelse)


        klageRepository.upsertFormkrav(vedtakId, FormkravOppfylt.IKKE_OPPFYLT, formkravBegrunnelse)
        val lagretKlageIkkeOppfylt = klageRepository.hentKlageBehandling(vedtakId)
        assertNotNull(lagretKlageIkkeOppfylt)
        assertEquals(FormkravOppfylt.IKKE_OPPFYLT, lagretKlageIkkeOppfylt.formkravOppfylt)
        assertEquals(formkravBegrunnelse, lagretKlageIkkeOppfylt.formkravBegrunnelse)

    }


    private fun opprettEnDefaultKlage(vedtakId: Long): OpprettKlageRequest {
        val norskIdent = "12345678910"
        val veilederIdent = "Z123456"
        return OpprettKlageRequest(vedtakId, Fnr(norskIdent), veilederIdent)
    }

}
