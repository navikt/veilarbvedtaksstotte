package no.nav.veilarbvedtaksstotte.service

import no.nav.common.types.identer.EnhetId
import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.client.arena.VeilarbarenaClient
import no.nav.veilarbvedtaksstotte.client.dokarkiv.DokarkivClient
import no.nav.veilarbvedtaksstotte.client.dokarkiv.OpprettJournalpostDTO
import no.nav.veilarbvedtaksstotte.client.dokarkiv.OpprettetJournalpostDTO
import no.nav.veilarbvedtaksstotte.client.dokdistfordeling.DistribuerJournalpostDTO
import no.nav.veilarbvedtaksstotte.client.dokdistfordeling.DistribuerJournalpostResponsDTO
import no.nav.veilarbvedtaksstotte.client.dokdistfordeling.DokdistribusjonClient
import no.nav.veilarbvedtaksstotte.client.dokument.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class DokumentServiceV2(
    val veilarbdokumentClient: VeilarbdokumentClient,
    val veilarbarenaClient: VeilarbarenaClient,
    val dokarkivClient: DokarkivClient,
    val dokdistribusjonClient: DokdistribusjonClient
) {

    val log = LoggerFactory.getLogger(DokumentServiceV2::class.java)

    fun produserDokument(sendDokumentDTO: SendDokumentDTO, utkast: Boolean): ByteArray {
        val produserDokumentV2DTO = sendDokumentDTO.let {
            ProduserDokumentV2DTO(
                brukerFnr = it.brukerFnr,
                malType = it.malType,
                enhetId = it.enhetId,
                begrunnelse = it.begrunnelse,
                opplysninger = it.opplysninger,
                utkast = utkast
            )
        }
        return veilarbdokumentClient.produserDokumentV2(produserDokumentV2DTO)
    }

    fun produserOgJournalforDokument(sendDokumentDTO: SendDokumentDTO, referanse: UUID): OpprettetJournalpostDTO {
        val dokument = produserDokument(sendDokumentDTO = sendDokumentDTO, utkast = false)
        val tittel = "Vurdering av ditt behov for oppfølging fra NAV"
        val oppfolgingssak = veilarbarenaClient.oppfolgingssak(sendDokumentDTO.brukerFnr)
        return journalforDokument(
            tittel = tittel,
            enhetId = sendDokumentDTO.enhetId,
            fnr = sendDokumentDTO.brukerFnr,
            oppfolgingssak = oppfolgingssak,
            malType = sendDokumentDTO.malType,
            dokument = dokument,
            referanse = referanse
        )
    }

    fun journalforDokument(
        tittel: String,
        enhetId: EnhetId,
        fnr: Fnr,
        oppfolgingssak: String,
        malType: MalType,
        dokument: ByteArray,
        referanse: UUID
    ): OpprettetJournalpostDTO {

        val request = OpprettJournalpostDTO(
            tittel = tittel,
            journalpostType = OpprettJournalpostDTO.JournalpostType.UTGAAENDE,
            tema = "OPP",
            journalfoerendeEnhet = enhetId,
            eksternReferanseId = referanse.toString(),
            avsenderMottaker = OpprettJournalpostDTO.AvsenderMottaker(
                id = fnr.get(),
                idType = OpprettJournalpostDTO.AvsenderMottaker.IdType.FNR
            ),
            bruker = OpprettJournalpostDTO.Bruker(
                id = fnr.get(),
                idType = OpprettJournalpostDTO.Bruker.IdType.FNR
            ),
                sak = OpprettJournalpostDTO.Sak(
                fagsakId = oppfolgingssak,
                fagsaksystem = "AO01", // Arena-kode, siden oppfølgingssaken er fra Arena
                sakstype = OpprettJournalpostDTO.Sak.Type.FAGSAK
            ),
            dokumenter = listOf(
                OpprettJournalpostDTO.Dokument(
                    tittel = tittel,
                    brevkode = malType.name,
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
                dokumentProdApp = "VEILARB_VEDTAK14A" // for sporing og feilsøking
            )
        )
    }

}
