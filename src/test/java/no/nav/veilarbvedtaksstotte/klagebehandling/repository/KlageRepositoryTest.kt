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
            generellData = GenerellData(
                vedtakId = vedtakId,
                norskIdent = norskIdent,
                veilederIdent = veilederIdent,
                klageDato = klageDato,
                klageJournalpostid = journalpostId
            )
        )
        val oppdatertRequest =
            KlageBehandling(
                generellData = GenerellData(
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
        assertEquals(vedtakId, lagretKlage.generellData.vedtakId)
        assertEquals(norskIdent, lagretKlage.generellData.norskIdent)
        assertEquals(veilederIdent, lagretKlage.generellData.veilederIdent)
        assertEquals(klageDato, lagretKlage.generellData.klageDato)
        assertEquals(journalpostId, lagretKlage.generellData.klageJournalpostid)
        assertEquals(Resultat.IKKE_SATT, lagretKlage.resultatData?.resultat)
        assertEquals(FormkravOppfylt.IKKE_SATT, lagretKlage.formkravData?.formkravOppfylt)
        assertEquals(Status.UTKAST, lagretKlage.resultatData?.status)

        klageRepository.upsertKlagebehandling(oppdatertRequest)
        val lagretKlageOppdatert = klageRepository.hentKlageBehandling(vedtakId)
        assertNotNull(lagretKlageOppdatert)
        assertEquals(nyVeilederIdent, lagretKlageOppdatert.generellData.veilederIdent)
        assertEquals(klageDato.minusDays(1), lagretKlageOppdatert.generellData.klageDato)
    }


    @Test
    fun `updateFormkrav skal oppdatere felt for formkrav`() {
        val vedtakId: Long = 111222333
        val formkravBegrunnelse = "Alle formkrav er oppfylt."

        val formkrav = FormkravData(
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
        assertEquals(FormkravOppfylt.OPPFYLT, lagretKlageOppfylt.formkravData?.formkravOppfylt)
        assertEquals(FormkravSvar.JA, lagretKlageOppfylt.formkravData?.formkravSignert)
        assertEquals(FormkravSvar.JA, lagretKlageOppfylt.formkravData?.formkravPart)
        assertEquals(FormkravSvar.JA, lagretKlageOppfylt.formkravData?.formkravKonkret)
        assertEquals(FormkravSvar.NEI, lagretKlageOppfylt.formkravData?.formkravKlagefristOpprettholdt)
        assertEquals(
            FormkravKlagefristUnntakSvar.JA_SAERLIGE_GRUNNER,
            lagretKlageOppfylt.formkravData?.formkravKlagefristUnntak
        )
        assertNull(lagretKlageOppfylt.formkravData?.formkravBegrunnelseBrev)
        assertEquals(formkravBegrunnelse, lagretKlageOppfylt.formkravData?.formkravBegrunnelseIntern)


        val endretFormkrav = formkrav.copy(
            formkravKonkret = FormkravSvar.NEI,
            formkravBegrunnelseIntern = "Det klages ikke på noe konkret i saken.",
            formkravBegrunnelseBrev = "Det klages ikke på noe konkret i saken."
        )

        klageRepository.updateFormkrav(vedtakId, endretFormkrav, FormkravOppfylt.IKKE_OPPFYLT)
        val lagretKlageIkkeOppfylt = klageRepository.hentKlageBehandling(vedtakId)
        assertNotNull(lagretKlageIkkeOppfylt)
        assertEquals(FormkravOppfylt.IKKE_OPPFYLT, lagretKlageIkkeOppfylt.formkravData?.formkravOppfylt)
        assertEquals(
            "Det klages ikke på noe konkret i saken.",
            lagretKlageIkkeOppfylt.formkravData?.formkravBegrunnelseIntern
        )
        assertEquals(
            "Det klages ikke på noe konkret i saken.",
            lagretKlageIkkeOppfylt.formkravData?.formkravBegrunnelseBrev
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
        assertEquals(Resultat.AVVIST, lagretKlageOppfylt.resultatData?.resultat)
        assertEquals(begrunnelse, lagretKlageOppfylt.resultatData?.resultatBegrunnelse)
    }

    @Test
    fun `updateStatus skal oppdatere felt for status`() {
        val vedtakId: Long = 111222333
        val status = Status.SENDT_TIL_KABAL

        val defaultRequest = opprettEnDefaultKlage(vedtakId)
        klageRepository.upsertKlagebehandling(defaultRequest)
        klageRepository.updateStatus(vedtakId, status)

        val lagretKlageOppfylt = klageRepository.hentKlageBehandling(vedtakId)?.resultatData
        assertNotNull(lagretKlageOppfylt)
        assertEquals(Status.SENDT_TIL_KABAL, lagretKlageOppfylt.status)
    }

    private fun opprettEnDefaultKlage(vedtakId: Long): KlageBehandling {
        val norskIdent = "12345678910"
        val veilederIdent = "Z123456"
        val klageDato = LocalDate.of(2026, 2, 14)
        val journalpostId = "987654321"
        return KlageBehandling(
            generellData = GenerellData(
                vedtakId,
                norskIdent,
                veilederIdent,
                klageDato,
                journalpostId
            )
        )
    }

}
