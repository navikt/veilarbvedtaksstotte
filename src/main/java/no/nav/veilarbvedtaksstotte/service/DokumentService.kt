package no.nav.veilarbvedtaksstotte.service

import no.nav.common.client.norg2.Enhet
import no.nav.common.types.identer.EnhetId
import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.client.pdf.PdfClient
import no.nav.veilarbvedtaksstotte.client.pdf.PdfClient.Adresse.Companion.fraEnhetPostadresse
import no.nav.veilarbvedtaksstotte.client.arena.VeilarbarenaClient
import no.nav.veilarbvedtaksstotte.client.dokarkiv.DokarkivClient
import no.nav.veilarbvedtaksstotte.client.dokarkiv.OpprettJournalpostDTO
import no.nav.veilarbvedtaksstotte.client.dokarkiv.OpprettetJournalpostDTO
import no.nav.veilarbvedtaksstotte.client.dokument.MalType
import no.nav.veilarbvedtaksstotte.client.dokument.ProduserDokumentDTO
import no.nav.veilarbvedtaksstotte.client.norg2.EnhetKontaktinformasjon
import no.nav.veilarbvedtaksstotte.client.person.VeilarbpersonClient
import no.nav.veilarbvedtaksstotte.client.regoppslag.RegoppslagClient
import no.nav.veilarbvedtaksstotte.client.regoppslag.RegoppslagRequestDTO
import no.nav.veilarbvedtaksstotte.client.regoppslag.RegoppslagResponseDTO.AdresseType.UTENLANDSKPOSTADRESSE
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.VeilarbveilederClient
import no.nav.veilarbvedtaksstotte.domain.Målform
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak
import no.nav.veilarbvedtaksstotte.utils.DateFormatters
import no.nav.veilarbvedtaksstotte.utils.StringUtils.splitNewline
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.*
import java.util.function.Supplier

@Service
class DokumentService(
    val regoppslagClient: RegoppslagClient,
    val pdfClient: PdfClient,
    val veilarbarenaClient: VeilarbarenaClient,
    val veilarbpersonClient: VeilarbpersonClient,
    val veilarbveilederClient: VeilarbveilederClient,
    val dokarkivClient: DokarkivClient,
    val enhetInfoService: EnhetInfoService,
    val malTypeService: MalTypeService
) {

    val log = LoggerFactory.getLogger(DokumentService::class.java)

    fun produserDokumentutkast(vedtak: Vedtak, fnr: Fnr): ByteArray {
        val produserDokumentDTO = lagProduserDokumentDTO(vedtak = vedtak, fnr = fnr, utkast = true)
        return produserDokument(produserDokumentDTO)
    }

    fun produserOgJournalforDokument(vedtak: Vedtak, fnr: Fnr, referanse: UUID): OpprettetJournalpostDTO {
        val produserDokumentDTO = lagProduserDokumentDTO(vedtak = vedtak, fnr = fnr, utkast = false)
        val dokument = produserDokument(produserDokumentDTO)
        val tittel = "Vurdering av ditt behov for oppfølging fra NAV"
        val oppfolgingssak = veilarbarenaClient.oppfolgingssak(fnr)
            .orElseThrow { throw IllegalStateException("Det finnes ingen oppfolgingssak i arena for vedtak id: ${vedtak.id}") }

        return journalforDokument(
            tittel = tittel,
            enhetId = produserDokumentDTO.enhetId,
            fnr = fnr,
            oppfolgingssak = oppfolgingssak,
            malType = produserDokumentDTO.malType,
            dokument = dokument,
            referanse = referanse
        )
    }

    fun produserDokument(dto: ProduserDokumentDTO): ByteArray {

        val brevdataOppslag = hentBrevdata(dto.brukerFnr, dto.enhetId, dto.veilederIdent)
        val brevdata = mapBrevdata(dto, brevdataOppslag)

        return pdfClient.genererPdf(brevdata)
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

    private fun lagProduserDokumentDTO(vedtak: Vedtak, fnr: Fnr, utkast: Boolean): ProduserDokumentDTO {
        val postadresse = regoppslagClient.hentPostadresse(
            RegoppslagRequestDTO(
                ident = fnr.get(), tema = "OPP"
            )
        )
        val malType = malTypeService.utledMalTypeFraVedtak(vedtak, fnr)

        return ProduserDokumentDTO(
            brukerFnr = fnr,
            navn = postadresse.navn,
            malType = malType,
            enhetId = EnhetId.of(vedtak.oppfolgingsenhetId),
            veilederIdent = vedtak.veilederIdent,
            begrunnelse = vedtak.begrunnelse,
            opplysninger = vedtak.opplysninger,
            utkast = utkast,
            adresse = ProduserDokumentDTO.AdresseDTO(
                adresselinje1 = postadresse.adresse.adresselinje1,
                adresselinje2 = postadresse.adresse.adresselinje2,
                adresselinje3 = postadresse.adresse.adresselinje3,
                postnummer = postadresse.adresse.postnummer,
                poststed = postadresse.adresse.poststed,
                land = if (postadresse.adresse.type == UTENLANDSKPOSTADRESSE) postadresse.adresse.land else null
            )
        )
    }

    data class BrevdataOppslag(val enhetKontaktinformasjon: EnhetKontaktinformasjon,
                               val målform: Målform,
                               val veilederNavn: String,
                               val enhet: Enhet,
                               val kontaktEnhet: Enhet)

    private fun hentBrevdata(fnr: Fnr, enhetId: EnhetId, veilederIdent: String): BrevdataOppslag {
        val enhetKontaktinformasjon: EnhetKontaktinformasjon = enhetInfoService.utledEnhetKontaktinformasjon(enhetId)
        val målform = veilarbpersonClient.hentMålform(fnr)
        val veileder = veilarbveilederClient.hentVeileder(veilederIdent)

        val enhet: Enhet = enhetInfoService.hentEnhet(enhetId)
        val kontaktEnhet: Enhet = enhetInfoService.hentEnhet(enhetKontaktinformasjon.enhetNr)

        return BrevdataOppslag(
            enhetKontaktinformasjon = enhetKontaktinformasjon,
            målform = målform,
            veilederNavn = veileder.navn,
            enhet = enhet,
            kontaktEnhet = kontaktEnhet
        )
    }

    companion object {

        fun mapBrevdata(dto: ProduserDokumentDTO, brevdataOppslag: BrevdataOppslag): PdfClient.Brevdata {

            val mottaker = PdfClient.Mottaker(
                navn = dto.navn,
                adresselinje1 = dto.adresse.adresselinje1,
                adresselinje2 = dto.adresse.adresselinje2,
                adresselinje3 = dto.adresse.adresselinje3,
                postnummer = dto.adresse.postnummer,
                poststed = dto.adresse.poststed,
                land = dto.adresse.land
            )
            val dato = LocalDate.now().format(DateFormatters.NORSK_DATE)
            val postadresse = fraEnhetPostadresse(brevdataOppslag.enhetKontaktinformasjon.postadresse)


            val enhetNavn = brevdataOppslag.enhet.navn ?: throw IllegalStateException(
                "Manglende navn for enhet ${brevdataOppslag.enhet.enhetNr}"
            )

            val kontaktEnhetNavn = brevdataOppslag.kontaktEnhet.navn ?: throw IllegalStateException(
                "Manglende navn for enhet ${brevdataOppslag.kontaktEnhet.enhetNr}"
            )

            val telefonnummer = brevdataOppslag.enhetKontaktinformasjon.telefonnummer ?: throw IllegalStateException(
                "Manglende telefonnummer for enhet ${brevdataOppslag.enhetKontaktinformasjon.enhetNr}"
            )

            val begrunnelseAvsnitt =
                dto.begrunnelse?.let { splitNewline(it) }?.filterNot { it.isEmpty() } ?: emptyList()

            return PdfClient.Brevdata(
                malType = dto.malType,
                veilederNavn = brevdataOppslag.veilederNavn,
                navKontor = enhetNavn,
                kontaktEnhetNavn = kontaktEnhetNavn,
                kontaktTelefonnummer = telefonnummer,
                dato = dato,
                malform = brevdataOppslag.målform,
                mottaker = mottaker,
                postadresse = postadresse,
                begrunnelse = begrunnelseAvsnitt,
                kilder = dto.opplysninger,
                utkast = dto.utkast
            )
        }
    }

}
