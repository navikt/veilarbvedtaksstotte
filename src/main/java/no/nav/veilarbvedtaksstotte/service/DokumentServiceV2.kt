package no.nav.veilarbvedtaksstotte.service

import no.nav.common.types.identer.EnhetId
import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.client.arena.VeilarbarenaClient
import no.nav.veilarbvedtaksstotte.client.dokarkiv.DokarkivClient
import no.nav.veilarbvedtaksstotte.client.dokarkiv.OpprettJournalpostDTO
import no.nav.veilarbvedtaksstotte.client.dokarkiv.OpprettetJournalpostDTO
import no.nav.veilarbvedtaksstotte.client.dokument.MalType
import no.nav.veilarbvedtaksstotte.client.dokument.ProduserDokumentV2DTO
import no.nav.veilarbvedtaksstotte.client.dokument.VeilarbdokumentClient
import no.nav.veilarbvedtaksstotte.client.regoppslag.RegoppslagClient
import no.nav.veilarbvedtaksstotte.client.regoppslag.RegoppslagRequestDTO
import no.nav.veilarbvedtaksstotte.client.regoppslag.RegoppslagResponseDTO.AdresseType.UTENLANDSKPOSTADRESSE
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
class DokumentServiceV2(
    val regoppslagClient: RegoppslagClient,
    val veilarbdokumentClient: VeilarbdokumentClient,
    val veilarbarenaClient: VeilarbarenaClient,
    val dokarkivClient: DokarkivClient,
    val malTypeService: MalTypeService
) {

    val log = LoggerFactory.getLogger(DokumentServiceV2::class.java)

    fun produserDokumentutkast(vedtak: Vedtak, fnr: Fnr): ByteArray {
        val produserDokumentV2DTO = lagProduserDokumentDTO(vedtak = vedtak, fnr = fnr, utkast = true)
        return veilarbdokumentClient.produserDokumentV2(produserDokumentV2DTO)
    }

    fun produserOgJournalforDokument(vedtak: Vedtak, fnr: Fnr, referanse: UUID): OpprettetJournalpostDTO {
        val produserDokumentV2DTO = lagProduserDokumentDTO(vedtak = vedtak, fnr = fnr, utkast = false)
        val dokument = veilarbdokumentClient.produserDokumentV2(produserDokumentV2DTO)
        val tittel = "Vurdering av ditt behov for oppfølging fra NAV"
        val oppfolgingssak = veilarbarenaClient.oppfolgingssak(fnr)

        return journalforDokument(
            tittel = tittel,
            enhetId = produserDokumentV2DTO.enhetId,
            fnr = fnr,
            oppfolgingssak = oppfolgingssak,
            malType = produserDokumentV2DTO.malType,
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

    private fun lagProduserDokumentDTO(vedtak: Vedtak, fnr: Fnr, utkast: Boolean): ProduserDokumentV2DTO {
        val postadresse = regoppslagClient.hentPostadresse(
            RegoppslagRequestDTO(
                ident = fnr.get(), tema = "OPP"
            )
        )
        val malType = malTypeService.utledMalTypeFraVedtak(vedtak, fnr)

        return ProduserDokumentV2DTO(
            brukerFnr = fnr,
            navn = postadresse.navn,
            malType = malType,
            enhetId = EnhetId.of(vedtak.oppfolgingsenhetId),
            begrunnelse = vedtak.begrunnelse,
            opplysninger = vedtak.opplysninger,
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
    }
}
