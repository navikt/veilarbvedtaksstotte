package no.nav.veilarbvedtaksstotte.service

import no.nav.common.types.identer.EnhetId
import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.client.dokarkiv.DokarkivClient
import no.nav.veilarbvedtaksstotte.client.dokarkiv.OpprettJournalpostDTO
import no.nav.veilarbvedtaksstotte.client.dokarkiv.OpprettetJournalpostDTO
import no.nav.veilarbvedtaksstotte.client.dokdistfordeling.DistribuerJournalpostDTO
import no.nav.veilarbvedtaksstotte.client.dokdistfordeling.DistribuerJournalpostResponsDTO
import no.nav.veilarbvedtaksstotte.client.dokdistfordeling.DokdistribusjonClient
import no.nav.veilarbvedtaksstotte.client.dokument.VeilarbdokumentClient

class DokumentService(val veilarbdokumentClient: VeilarbdokumentClient,
                      val dokarkivClient: DokarkivClient,
                      val dokdistribusjonClient: DokdistribusjonClient) {

    fun journalforDokument(tittel: String,
                           enhetId: EnhetId,
                           fnr: Fnr,
                           oppfolgingssak: String,
                           brevkode: String,
                           dokument: ByteArray): OpprettetJournalpostDTO {

        val request = OpprettJournalpostDTO(
                tittel = tittel,
                journalpostType = OpprettJournalpostDTO.JournalpostType.UTGAAENDE,
                tema = "OPP",
                journalfoerendeEnhet = enhetId,
                avsenderMottaker = OpprettJournalpostDTO.AvsenderMottaker(
                        id = fnr.get(),
                        idType = OpprettJournalpostDTO.AvsenderMottaker.IdType.FNR
                ),
                bruker = OpprettJournalpostDTO.Bruker(
                        id = fnr.get(),
                        idType = OpprettJournalpostDTO.Bruker.IdType.FNR
                ),
                OpprettJournalpostDTO.Sak(
                        fagsakId = oppfolgingssak,
                        fagsaksystem = "AO01", // Arena-kode
                        sakstype = OpprettJournalpostDTO.Sak.Type.FAGSAK
                ),
                dokumenter = listOf(
                        OpprettJournalpostDTO.Dokument(
                                tittel = tittel,
                                brevkode = brevkode,
                                dokumentvarianter = listOf(
                                        OpprettJournalpostDTO.DokumentVariant(
                                                "PDFA",
                                                fysiskDokument = dokument,
                                                variantformat = "ARKIV"
                                        )
                                )
                        )
                )
        )

        return dokarkivClient.opprettJournalpost(request)
    }

    fun distribuerJournalpost(jounralpostId: String): DistribuerJournalpostResponsDTO {
        return dokdistribusjonClient.distribuerJournalpost(
                DistribuerJournalpostDTO(
                        journalpostId = jounralpostId,
                        bestillendeFagsystem = "BD11", // veilarb-kode
                        dokumentProdApp = "TODO"))
    }

}
