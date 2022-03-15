package no.nav.veilarbvedtaksstotte.service

import no.nav.common.types.identer.EnhetId
import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.client.arena.VeilarbarenaClient
import no.nav.veilarbvedtaksstotte.client.dokarkiv.DokarkivClient
import no.nav.veilarbvedtaksstotte.client.dokarkiv.OpprettJournalpostDTO
import no.nav.veilarbvedtaksstotte.client.dokarkiv.OpprettetJournalpostDTO
import no.nav.veilarbvedtaksstotte.client.dokument.MalType
import no.nav.veilarbvedtaksstotte.client.dokument.ProduserDokumentV2DTO
import no.nav.veilarbvedtaksstotte.client.dokument.SendDokumentDTO
import no.nav.veilarbvedtaksstotte.client.dokument.VeilarbdokumentClient
import no.nav.veilarbvedtaksstotte.client.regoppslag.RegoppslagClient
import no.nav.veilarbvedtaksstotte.client.regoppslag.RegoppslagRequestDTO
import no.nav.veilarbvedtaksstotte.client.regoppslag.RegoppslagResponseDTO.AdresseType.UTENLANDSKPOSTADRESSE
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
class DokumentServiceV2(
    val regoppslagClient: RegoppslagClient,
    val veilarbdokumentClient: VeilarbdokumentClient,
    val veilarbarenaClient: VeilarbarenaClient,
    val dokarkivClient: DokarkivClient,
) {

    val log = LoggerFactory.getLogger(DokumentServiceV2::class.java)

    fun produserDokument(sendDokumentDTO: SendDokumentDTO, utkast: Boolean): ByteArray {
        val postadresse = regoppslagClient.hentPostadresse(
            RegoppslagRequestDTO(
                ident = sendDokumentDTO.brukerFnr.get(), tema = "OPP"
            )
        )

        val produserDokumentV2DTO =
            ProduserDokumentV2DTO(
                brukerFnr = sendDokumentDTO.brukerFnr,
                navn = postadresse.navn,
                malType = sendDokumentDTO.malType,
                enhetId = sendDokumentDTO.enhetId,
                begrunnelse = sendDokumentDTO.begrunnelse,
                opplysninger = sendDokumentDTO.opplysninger,
                utkast = utkast,
                adresse = ProduserDokumentV2DTO.AdresseDTO(
                    adresselinje1 = postadresse.adresse.adresselinje1,
                    adresselinje2 = postadresse.adresse.adresselinje2,
                    adresselinje3 = postadresse.adresse.adresselinje3,
                    postnummer = postadresse.adresse.postnummer,
                    poststed = postadresse.adresse.poststed,
                    land = if (postadresse.adresse.type == UTENLANDSKPOSTADRESSE) postadresse.adresse.land else null
                )
            )

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
                id = fnr.get(), idType = OpprettJournalpostDTO.AvsenderMottaker.IdType.FNR
            ),
            bruker = OpprettJournalpostDTO.Bruker(
                id = fnr.get(), idType = OpprettJournalpostDTO.Bruker.IdType.FNR
            ),
            sak = OpprettJournalpostDTO.Sak(
                fagsakId = oppfolgingssak, fagsaksystem = "AO01", // Arena-kode, siden oppfølgingssaken er fra Arena
                sakstype = OpprettJournalpostDTO.Sak.Type.FAGSAK
            ),
            dokumenter = listOf(
                OpprettJournalpostDTO.Dokument(
                    tittel = tittel, brevkode = malType.name, dokumentvarianter = listOf(
                        OpprettJournalpostDTO.DokumentVariant(
                            "PDFA", fysiskDokument = dokument, variantformat = "ARKIV"
                        )
                    )
                )
            )
        )

        return dokarkivClient.opprettJournalpost(request)
    }
}
