package no.nav.veilarbvedtaksstotte.klagebehandling.repository

import no.nav.veilarbvedtaksstotte.klagebehandling.controller.FormkravKlagefristUnntakSvar
import no.nav.veilarbvedtaksstotte.klagebehandling.controller.FormkravSvar
import no.nav.veilarbvedtaksstotte.klagebehandling.domene.*
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
        val request = KlageBehandling(
            klageInitiellData = KlageInitiellData(
                vedtakId = vedtakId,
                norskIdent = norskIdent,
                veilederIdent = veilederIdent,
                klageDato = klageDato,
                klageJournalpostid = journalpostId
            )
        )
        val oppdatertRequest =
            KlageBehandling(
                klageInitiellData = KlageInitiellData(
                    vedtakId = vedtakId,
                    norskIdent = norskIdent,
                    veilederIdent = nyVeilederIdent,
                    klageDato = klageDato.minusDays(1),
                    klageJournalpostid = journalpostId
                )
            )

        klageRepository.upsertKlagebehandling(request)
        val lagretKlage = klageRepository.hentKlageBehandling(vedtakId)
        assertNotNull(lagretKlage)
        assertEquals(vedtakId, lagretKlage.klageInitiellData.vedtakId)
        assertEquals(norskIdent, lagretKlage.klageInitiellData.norskIdent)
        assertEquals(veilederIdent, lagretKlage.klageInitiellData.veilederIdent)
        assertEquals(klageDato, lagretKlage.klageInitiellData.klageDato)
        assertEquals(journalpostId, lagretKlage.klageInitiellData.klageJournalpostid)
        assertEquals(Resultat.IKKE_SATT, lagretKlage.klageResultatData?.resultat)
        assertEquals(FormkravOppfylt.IKKE_SATT, lagretKlage.klageFormkravData?.formkravOppfylt)
        assertEquals(Status.UTKAST, lagretKlage.klageResultatData?.status)

        klageRepository.upsertKlagebehandling(oppdatertRequest)
        val lagretKlageOppdatert = klageRepository.hentKlageBehandling(vedtakId)
        assertNotNull(lagretKlageOppdatert)
        assertEquals(nyVeilederIdent, lagretKlageOppdatert.klageInitiellData.veilederIdent)
        assertEquals(klageDato.minusDays(1), lagretKlageOppdatert.klageInitiellData.klageDato)
    }


    @Test
    fun `updateFormkrav skal oppdatere felt for formkrav`() {
        val vedtakId: Long = 111222333
        val formkravBegrunnelse = "Alle formkrav er oppfylt."

        val formkrav = KlageFormkravData(
            formkravSignert = FormkravSvar.JA,
            formkravPart = FormkravSvar.JA,
            formkravKonkret = FormkravSvar.JA,
            formkravKlagefristOpprettholdt = FormkravSvar.NEI,
            formkravKlagefristUnntak = FormkravKlagefristUnntakSvar.JA_SAERLIGE_GRUNNER,
            formkravBegrunnelseIntern = formkravBegrunnelse,
            formkravBegrunnelseBrev = null
        )

        val defaultRequest = opprettEnDefaultKlage(vedtakId)
        klageRepository.upsertKlagebehandling(defaultRequest)
        klageRepository.updateFormkrav(vedtakId, formkrav, FormkravOppfylt.OPPFYLT)

        val lagretKlageOppfylt = klageRepository.hentKlageBehandling(vedtakId)
        assertNotNull(lagretKlageOppfylt)
        assertEquals(FormkravOppfylt.OPPFYLT, lagretKlageOppfylt.klageFormkravData?.formkravOppfylt)
        assertEquals(FormkravSvar.JA, lagretKlageOppfylt.klageFormkravData?.formkravSignert)
        assertEquals(FormkravSvar.JA, lagretKlageOppfylt.klageFormkravData?.formkravPart)
        assertEquals(FormkravSvar.JA, lagretKlageOppfylt.klageFormkravData?.formkravKonkret)
        assertEquals(FormkravSvar.NEI, lagretKlageOppfylt.klageFormkravData?.formkravKlagefristOpprettholdt)
        assertEquals(
            FormkravKlagefristUnntakSvar.JA_SAERLIGE_GRUNNER,
            lagretKlageOppfylt.klageFormkravData?.formkravKlagefristUnntak
        )
        assertNull(lagretKlageOppfylt.klageFormkravData?.formkravBegrunnelseBrev)
        assertEquals(formkravBegrunnelse, lagretKlageOppfylt.klageFormkravData?.formkravBegrunnelseIntern)


        val endretFormkrav = formkrav.copy(
            formkravKonkret = FormkravSvar.NEI,
            formkravBegrunnelseIntern = "Det klages ikke på noe konkret i saken.",
            formkravBegrunnelseBrev = "Det klages ikke på noe konkret i saken."
        )

        klageRepository.updateFormkrav(vedtakId, endretFormkrav, FormkravOppfylt.IKKE_OPPFYLT)
        val lagretKlageIkkeOppfylt = klageRepository.hentKlageBehandling(vedtakId)
        assertNotNull(lagretKlageIkkeOppfylt)
        assertEquals(FormkravOppfylt.IKKE_OPPFYLT, lagretKlageIkkeOppfylt.klageFormkravData?.formkravOppfylt)
        assertEquals(
            "Det klages ikke på noe konkret i saken.",
            lagretKlageIkkeOppfylt.klageFormkravData?.formkravBegrunnelseIntern
        )
        assertEquals(
            "Det klages ikke på noe konkret i saken.",
            lagretKlageIkkeOppfylt.klageFormkravData?.formkravBegrunnelseBrev
        )
    }

    @Test
    fun `updateResultat skal oppdatere felt for resultat`() {
        val vedtakId: Long = 111222333
        val resultat = Resultat.AVVIST
        val begrunnelse = "Formkrav for klagefrist er ikke oppfylt."

        val defaultRequest = opprettEnDefaultKlage(vedtakId)
        klageRepository.upsertKlagebehandling(defaultRequest)
        klageRepository.updateResultat(vedtakId, resultat, begrunnelse)

        val lagretKlageOppfylt = klageRepository.hentKlageBehandling(vedtakId)
        assertNotNull(lagretKlageOppfylt)
        assertEquals(Resultat.AVVIST, lagretKlageOppfylt.klageResultatData?.resultat)
        assertEquals(begrunnelse, lagretKlageOppfylt.klageResultatData?.resultatBegrunnelse)
    }

    @Test
    fun `updateStatus skal oppdatere felt for status`() {
        val vedtakId: Long = 111222333
        val status = Status.SENDT_TIL_KABAL

        val defaultRequest = opprettEnDefaultKlage(vedtakId)
        klageRepository.upsertKlagebehandling(defaultRequest)
        klageRepository.updateStatus(vedtakId, status)

        val lagretKlageOppfylt = klageRepository.hentKlageBehandling(vedtakId)?.klageResultatData
        assertNotNull(lagretKlageOppfylt)
        assertEquals(Status.SENDT_TIL_KABAL, lagretKlageOppfylt.status)
    }

    private fun opprettEnDefaultKlage(vedtakId: Long): KlageBehandling {
        val norskIdent = "12345678910"
        val veilederIdent = "Z123456"
        val klageDato = LocalDate.of(2026, 2, 14)
        val journalpostId = "987654321"
        return KlageBehandling(
            klageInitiellData = KlageInitiellData(
                vedtakId,
                norskIdent,
                veilederIdent,
                klageDato,
                journalpostId
            )
        )
    }

}
