package no.nav.veilarbvedtaksstotte.client.pdf

import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.client.person.OpplysningerOmArbeidssoekerMedProfilering
import no.nav.veilarbvedtaksstotte.client.person.OpplysningerOmArbeidssoekerResponse
import no.nav.veilarbvedtaksstotte.client.person.ProfileringResponse
import no.nav.veilarbvedtaksstotte.client.dokument.MalType
import no.nav.veilarbvedtaksstotte.client.person.dto.*
import no.nav.veilarbvedtaksstotte.domain.Malform
import no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.EgenvurderingDto
import java.time.ZonedDateTime


data class BrevdataDto(
    val malType: MalType,
    val veilederNavn: String,
    val navKontor: String,
    val dato: String,
    val malform: Malform,
    val begrunnelse: List<String>,
    val kilder: List<String>,
    val mottaker: Mottaker,
    val utkast: Boolean,
    val ungdomsgaranti: Boolean
)

data class Mottaker(
    val navn: String,
    val fodselsnummer: Fnr,
)

data class CvInnholdMedMottakerDto(
    val mottaker: Mottaker,
    val sistEndret: ZonedDateTime?,
    val synligForArbeidsgiver: Boolean?,
    val sistEndretAvNav: Boolean? = false,
    val sammendrag: String?,
    val arbeidserfaring: List<ArbeidserfaringDtoV2>?,
    val utdanning: List<UtdanningDtoV2>?,
    val fagdokumentasjoner: List<FagdokumentasjonDtoV2>?,
    val godkjenninger: List<GodkjenningDtoV2>?,
    val annenErfaring: List<AnnenErfaringDtoV2>?,
    val forerkort: List<ForerkortDtoV2>?,
    val kurs: List<KursDtoV2>?,
    val sertifikater: List<SertifikatDtoV2>?,
    val andreGodkjenninger: List<SertifikatDtoV2>?,
    val sprak: List<SprakDtoV2>?,
    val jobbprofil: FOJobbprofilDtoV2?
) {
    companion object {
        fun from(cv: CvInnhold, mottaker: Mottaker): CvInnholdMedMottakerDto {
            val rensetCv = cv.sanitize()

            return CvInnholdMedMottakerDto(
                mottaker = Mottaker(mottaker.navn, mottaker.fodselsnummer),
                sistEndret = rensetCv.sistEndret,
                synligForArbeidsgiver = rensetCv.synligForArbeidsgiver,
                sistEndretAvNav = rensetCv.sistEndretAvNav,
                sammendrag = rensetCv.sammendrag,
                arbeidserfaring = rensetCv.arbeidserfaring,
                utdanning = rensetCv.utdanning,
                fagdokumentasjoner = rensetCv.fagdokumentasjoner,
                godkjenninger = rensetCv.godkjenninger,
                annenErfaring = rensetCv.annenErfaring,
                forerkort = rensetCv.forerkort,
                kurs = rensetCv.kurs,
                sertifikater = rensetCv.sertifikater,
                andreGodkjenninger = rensetCv.andreGodkjenninger,
                sprak = rensetCv.sprak,
                jobbprofil = rensetCv.jobbprofil
            )
        }
    }
}

data class EgenvurderingMedMottakerDto(
    val mottaker: Mottaker,
    val sistOppdatert: String? = null,
    val svar: List<EgenvurderingDto.Svar>? = null
) {
    companion object {
        fun from(egenvurdering: EgenvurderingDto, mottaker: Mottaker) = EgenvurderingMedMottakerDto(
            mottaker = mottaker,
            sistOppdatert = egenvurdering.sistOppdatert,
            svar = egenvurdering.svar
        )
    }
}

data class OpplysningerOmArbeidssoekerMedProfileringMedMottakerDto(
    val mottaker: Mottaker,
    val opplysningerOmArbeidssoeker: OpplysningerOmArbeidssoekerResponse? = null,
    val profilering: ProfileringResponse? = null,
    val arbeidssoekerperiodeStartet: ZonedDateTime? = null
) {
    companion object {
        fun from(registreringsdata: OpplysningerOmArbeidssoekerMedProfilering, mottaker: Mottaker) =
            OpplysningerOmArbeidssoekerMedProfileringMedMottakerDto(
                mottaker = mottaker,
                opplysningerOmArbeidssoeker = registreringsdata.opplysningerOmArbeidssoeker,
                profilering = registreringsdata.profilering,
                arbeidssoekerperiodeStartet = registreringsdata.arbeidssoekerperiodeStartet
            )
    }
}

