package no.nav.veilarbvedtaksstotte.klagebehandling.repository

import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.klagebehandling.domene.FormkravOppfylt
import no.nav.veilarbvedtaksstotte.klagebehandling.domene.Resultat
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
        val klageDato = LocalDate.of(2026, 2, 14)
        val journalpostId = "987654321"
        val request = OpprettKlageRequest(vedtakId, Fnr(norskIdent), veilederIdent, klageDato, journalpostId)
        val oppdatertRequest =
            OpprettKlageRequest(vedtakId, Fnr(norskIdent), nyVeilederIdent, klageDato.minusDays(1), journalpostId)

        klageRepository.upsertOpprettKlagebehandling(request)
        val lagretKlage = klageRepository.hentKlageBehandling(vedtakId)
        assertNotNull(lagretKlage)
        assertEquals(vedtakId, lagretKlage.vedtakId)
        assertEquals(norskIdent, lagretKlage.norskIdent)
        assertEquals(veilederIdent, lagretKlage.veilederIdent)
        assertEquals(klageDato, lagretKlage.klageDato)
        assertEquals(journalpostId, lagretKlage.klageJournalpostid)
        assertEquals(Resultat.IKKE_SATT, lagretKlage.resultat)
        assertEquals(FormkravOppfylt.IKKE_SATT, lagretKlage.formkravOppfylt)

        klageRepository.upsertOpprettKlagebehandling(oppdatertRequest)
        val lagretKlageOppdatert = klageRepository.hentKlageBehandling(vedtakId)
        assertNotNull(lagretKlageOppdatert)
        assertEquals(nyVeilederIdent, lagretKlageOppdatert.veilederIdent)
        assertEquals(klageDato.minusDays(1), lagretKlageOppdatert.klageDato)
    }


    @Test
    fun `upsertFormkrav skal oppdatere felt for formkravOppfylt og formkravBegrunnelse`() {
        val vedtakId: Long = 111222333
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

    @Test
    fun `upsertResultat skal oppdatere felt for resultat`() {
        val vedtakId: Long = 111222333
        val resultat = Resultat.AVVIST
        val begrunnelse = "Formkrav for klagefrist er ikke oppfylt."

        val defaultRequest = opprettEnDefaultKlage(vedtakId)
        klageRepository.upsertOpprettKlagebehandling(defaultRequest)
        klageRepository.upsertResultat(vedtakId, resultat, begrunnelse)

        val lagretKlageOppfylt = klageRepository.hentKlageBehandling(vedtakId)
        assertNotNull(lagretKlageOppfylt)
        assertEquals(Resultat.AVVIST, lagretKlageOppfylt.resultat)
        assertEquals(begrunnelse, lagretKlageOppfylt.resultatBegrunnelse)
    }


    private fun opprettEnDefaultKlage(vedtakId: Long): OpprettKlageRequest {
        val norskIdent = "12345678910"
        val veilederIdent = "Z123456"
        val klageDato = LocalDate.of(2026, 2, 14)
        val journalpostId = "987654321"
        return OpprettKlageRequest(vedtakId, Fnr(norskIdent), veilederIdent, klageDato, journalpostId)
    }

}
