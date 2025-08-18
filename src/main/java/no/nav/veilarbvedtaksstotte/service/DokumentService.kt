package no.nav.veilarbvedtaksstotte.service

import no.nav.common.client.norg2.Enhet
import no.nav.common.types.identer.EnhetId
import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.client.dokarkiv.DokarkivClient
import no.nav.veilarbvedtaksstotte.client.dokarkiv.request.OpprettJournalpostDTO
import no.nav.veilarbvedtaksstotte.client.dokarkiv.request.OpprettetJournalpostDTO
import no.nav.veilarbvedtaksstotte.client.dokument.MalType
import no.nav.veilarbvedtaksstotte.client.dokument.ProduserDokumentDTO
import no.nav.veilarbvedtaksstotte.client.norg2.EnhetKontaktinformasjon
import no.nav.veilarbvedtaksstotte.client.pdf.BrevdataDto
import no.nav.veilarbvedtaksstotte.client.pdf.Mottaker
import no.nav.veilarbvedtaksstotte.client.person.VeilarbpersonClient
import no.nav.veilarbvedtaksstotte.client.person.dto.FodselsdatoOgAr
import no.nav.veilarbvedtaksstotte.client.person.dto.VergeData
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.VeilarboppfolgingClient
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.dto.SakDTO
import no.nav.veilarbvedtaksstotte.domain.Malform
import no.nav.veilarbvedtaksstotte.domain.arkiv.BrevKode
import no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.OyeblikksbildePdfTemplate
import no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.OyeblikksbildeType
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak
import no.nav.veilarbvedtaksstotte.utils.DateFormatters
import no.nav.veilarbvedtaksstotte.utils.StringUtils.splitNewline
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.*
import kotlin.jvm.optionals.getOrElse


@Service
class DokumentService(
    val veilarboppfolgingClient: VeilarboppfolgingClient,
    val veilarbpersonClient: VeilarbpersonClient,
    val dokarkivClient: DokarkivClient,
    val malTypeService: MalTypeService,
    val oyeblikksbildeService: OyeblikksbildeService,
    val pdfService: PdfService,
) {

    val log = LoggerFactory.getLogger(DokumentService::class.java)
    fun produserDokumentutkast(vedtak: Vedtak, fnr: Fnr): ByteArray {
        val produserDokumentDTO = lagProduserDokumentDTO(vedtak = vedtak, fnr = fnr, utkast = true)
        return pdfService.produserDokument(produserDokumentDTO)
    }

    fun produserOgJournalforDokumenterForVedtak(vedtak: Vedtak, fnr: Fnr): OpprettetJournalpostDTO {
        val produserDokumentDTO = lagProduserDokumentDTO(vedtak = vedtak, fnr = fnr, utkast = false)
        val dokument = pdfService.produserDokument(produserDokumentDTO)
        val tittel = "Vurdering av ditt behov for oppfølging fra Nav"
        val mottaker = Mottaker(produserDokumentDTO.navn, fnr)

        val oppfolgingsperiode = veilarboppfolgingClient.hentGjeldendeOppfolgingsperiode(fnr)
        val oppfolgingssak = veilarboppfolgingClient.hentOppfolgingsperiodeSak(oppfolgingsperiode.get().uuid)

        val referanse = vedtak.getReferanse()

        val oyeblikksbildeForVedtak = oyeblikksbildeService.hentOyeblikksbildeForVedtakJournalforing(vedtak.id)

        val behovsVurderingData =
            oyeblikksbildeForVedtak.firstOrNull { it.oyeblikksbildeType == OyeblikksbildeType.EGENVURDERING }
        val cvData =
            oyeblikksbildeForVedtak.firstOrNull { it.oyeblikksbildeType == OyeblikksbildeType.CV_OG_JOBBPROFIL }
        val arbeidssokerRegistretData =
            oyeblikksbildeForVedtak.firstOrNull { it.oyeblikksbildeType == OyeblikksbildeType.ARBEIDSSOKERREGISTRET }

        val behovsVurderingPdf = pdfService.produserBehovsvurderingPdf(behovsVurderingData?.json, mottaker )
        val cvPDF = pdfService.produserCVPdf(cvData?.json, mottaker)
        val arbeidssokerRegistretPdf = pdfService.produserArbeidssokerRegistretPdf(arbeidssokerRegistretData?.json, mottaker)


        return journalforDokument(
            tittel = tittel,
            enhetId = produserDokumentDTO.enhetId,
            fnr = fnr,
            oppfolgingssak = oppfolgingssak,
            malType = produserDokumentDTO.malType,
            dokument = dokument,
            oyeblikksbildeBehovsvurderingDokument = behovsVurderingPdf.getOrElse { null },
            oyeblikksbildeCVDokument = cvPDF.getOrElse { null },
            oyeblikksbildeArbeidssokerRegistretDokument = arbeidssokerRegistretPdf.getOrElse { null },
            referanse = referanse
        )
    }


    fun journalforDokument(
        tittel: String,
        enhetId: EnhetId,
        fnr: Fnr,
        oppfolgingssak: SakDTO,
        malType: MalType,
        dokument: ByteArray,
        oyeblikksbildeBehovsvurderingDokument: ByteArray?,
        oyeblikksbildeCVDokument: ByteArray?,
        oyeblikksbildeArbeidssokerRegistretDokument: ByteArray?,
        referanse: UUID
    ): OpprettetJournalpostDTO {

        val dokumenterList = mutableListOf<OpprettJournalpostDTO.Dokument>()

        dokumenterList.add(
            OpprettJournalpostDTO.Dokument(
                tittel = tittel, brevkode = malType.name, dokumentvarianter = listOf(
                    OpprettJournalpostDTO.DokumentVariant(
                        "PDFA", fysiskDokument = dokument, variantformat = "ARKIV"
                    )
                )
            )
        )

        if (oyeblikksbildeArbeidssokerRegistretDokument != null) {
            dokumenterList.add(
                OpprettJournalpostDTO.Dokument(
                    tittel = OyeblikksbildePdfTemplate.ARBEIDSSOKERREGISTRET.fileName,
                    brevkode = BrevKode.of(OyeblikksbildeType.ARBEIDSSOKERREGISTRET).name,
                    dokumentvarianter = listOf(
                        OpprettJournalpostDTO.DokumentVariant(
                            "PDFA",
                            fysiskDokument = oyeblikksbildeArbeidssokerRegistretDokument,
                            variantformat = "ARKIV"
                        )
                    )
                )
            )
        }

        if (oyeblikksbildeCVDokument != null) {
            dokumenterList.add(
                OpprettJournalpostDTO.Dokument(
                    tittel = OyeblikksbildePdfTemplate.CV_OG_JOBBPROFIL.fileName,
                    brevkode = BrevKode.of(OyeblikksbildeType.CV_OG_JOBBPROFIL).name,
                    dokumentvarianter = listOf(
                        OpprettJournalpostDTO.DokumentVariant(
                            "PDFA", fysiskDokument = oyeblikksbildeCVDokument, variantformat = "ARKIV"
                        )
                    )
                )
            )
        }

        if (oyeblikksbildeBehovsvurderingDokument != null) {
            dokumenterList.add(
                OpprettJournalpostDTO.Dokument(
                    tittel = OyeblikksbildePdfTemplate.EGENVURDERING.fileName,
                    brevkode = BrevKode.of(OyeblikksbildeType.EGENVURDERING).name,
                    dokumentvarianter = listOf(
                        OpprettJournalpostDTO.DokumentVariant(
                            "PDFA", fysiskDokument = oyeblikksbildeBehovsvurderingDokument, variantformat = "ARKIV"
                        )
                    )
                )
            )
        }


        val request = OpprettJournalpostDTO(
            tittel = tittel,
            journalpostType = OpprettJournalpostDTO.JournalpostType.UTGAAENDE,
            tema = oppfolgingssak.tema,
            journalfoerendeEnhet = enhetId,
            eksternReferanseId = referanse.toString(),
            avsenderMottaker = OpprettJournalpostDTO.AvsenderMottaker(
                id = fnr.get(), idType = OpprettJournalpostDTO.AvsenderMottaker.IdType.FNR
            ),
            bruker = OpprettJournalpostDTO.Bruker(
                id = fnr.get(), idType = OpprettJournalpostDTO.Bruker.IdType.FNR
            ),
            sak = OpprettJournalpostDTO.Sak(
                fagsakId = oppfolgingssak.sakId.toString(), fagsaksystem = oppfolgingssak.fagsaksystem,
                sakstype = OpprettJournalpostDTO.Sak.Type.FAGSAK
            ),
            dokumenter = dokumenterList
        )

        return dokarkivClient.opprettJournalpost(request)
    }

    private fun lagProduserDokumentDTO(vedtak: Vedtak, fnr: Fnr, utkast: Boolean): ProduserDokumentDTO {
        val malType = malTypeService.utledMalTypeFraVedtak(vedtak, fnr)
        val personnavn = veilarbpersonClient.hentPersonNavnForJournalforing(fnr.toString())
        val navn = listOfNotNull(personnavn.fornavn, personnavn.mellomnavn, personnavn.etternavn).joinToString(" ")


        return ProduserDokumentDTO(
            brukerFnr = fnr,
            navn = navn,
            malType = malType,
            enhetId = EnhetId.of(vedtak.oppfolgingsenhetId),
            veilederIdent = vedtak.veilederIdent,
            begrunnelse = vedtak.begrunnelse,
            opplysninger = vedtak.opplysninger,
            utkast = utkast,
        )
    }

    data class BrevdataOppslag(
        val enhetKontaktinformasjon: EnhetKontaktinformasjon,
        val malform: Malform,
        val veilederNavn: String,
        val enhet: Enhet,
        val kontaktEnhet: Enhet,
        val fodselsdatoOgAr: FodselsdatoOgAr,
        val verge: VergeData
    )


    companion object {

        fun mapBrevdata(dto: ProduserDokumentDTO, brevdataOppslag: BrevdataOppslag): BrevdataDto {
            val dato = LocalDate.now().format(DateFormatters.NORSK_DATE)
            val erIAlderForUngdomsgaranti = erIAlderForUngdomsgaranti(brevdataOppslag.fodselsdatoOgAr)
            val harUngdomsgaranti = erIAlderForUngdomsgaranti &&
                    (dto.malType == MalType.SITUASJONSBESTEMT_INNSATS_BEHOLDE_ARBEID ||
                            dto.malType == MalType.SITUASJONSBESTEMT_INNSATS_SKAFFE_ARBEID ||
                            dto.malType == MalType.SPESIELT_TILPASSET_INNSATS_BEHOLDE_ARBEID ||
                            dto.malType == MalType.SPESIELT_TILPASSET_INNSATS_SKAFFE_ARBEID)

            val mottaker = Mottaker(
                navn = dto.navn,
                fodselsnummer = dto.brukerFnr,
            )

            val enhetNavn = brevdataOppslag.enhet.navn ?: throw IllegalStateException(
                "Manglende navn for enhet ${brevdataOppslag.enhet.enhetNr}"
            )

            val begrunnelseAvsnitt =
                dto.begrunnelse?.let { splitNewline(it) }?.filterNot { it.isEmpty() } ?: emptyList()

            // todo: i hvilke tilfeller skal navn vises på brevet?
            val vergenavn = if (brevdataOppslag.verge.vergemaalEllerFremtidsfullmakt.isEmpty()) {
                null
            } else {
                val sortertPåNyeste = brevdataOppslag.verge.vergemaalEllerFremtidsfullmakt.sortedByDescending { it.folkeregistermetadata?.gyldighetstidspunkt }
                val vergenavn = sortertPåNyeste.first().vergeEllerFullmektig?.navn
                val vergenavnString = vergenavn?.fornavn + (vergenavn?.mellomnavn?.let { " $it" } ?: "") + " " + vergenavn?.etternavn
                vergenavnString.ifBlank { null }
            }

            return BrevdataDto(
                malType = dto.malType,
                veilederNavn = brevdataOppslag.veilederNavn,
                navKontor = enhetNavn,
                dato = dato,
                malform = brevdataOppslag.malform,
                mottaker = mottaker,
                begrunnelse = begrunnelseAvsnitt,
                kilder = dto.opplysninger,
                utkast = dto.utkast,
                ungdomsgaranti = harUngdomsgaranti,
                vergenavn = vergenavn
            )
        }

        // Undomsgarantien gjelder for personer fra og med de fyller 16 inntil dagen de fyller 30 år.
        fun erIAlderForUngdomsgaranti(fodselsinfo: FodselsdatoOgAr): Boolean {
            val dagensDato = LocalDate.now()

            // Hvis fødselsdato er null, betyr det at vi kun har fødselsår. Per 17.6.25 gjelder dette kun 14 stk i pdl.
            // Tar derfor med hele året de fyller 16 eller 30 for å ta med alle uavhengig av når på året de er født.
            if (fodselsinfo.foedselsdato == null) {
                val blirDenneAldereIAr = dagensDato.year - fodselsinfo.foedselsaar
                return blirDenneAldereIAr in 16..30
            }

            val er16EllerOver = !fodselsinfo.foedselsdato.isAfter(dagensDato.minusYears(16))
            val erUnder30 = fodselsinfo.foedselsdato.isAfter(dagensDato.minusYears(30))
            return er16EllerOver && erUnder30
        }
    }

}
