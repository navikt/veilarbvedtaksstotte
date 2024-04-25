package no.nav.veilarbvedtaksstotte.client.person.dto

import java.time.LocalDate
import java.time.ZonedDateTime

data class CvInnhold(
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
)

data class SprakDtoV2(
    val sprak: String?,
    val muntligNiva: SprakferdighetDtoV2?,
    val skriftligNiva: SprakferdighetDtoV2?
)

enum class SprakferdighetDtoV2 {
    IKKE_OPPGITT,
    NYBEGYNNER,
    GODT,
    VELDIG_GODT,
    FOERSTESPRAAK
}

data class KursDtoV2(
    val tittel: String?,
    val arrangor: String?,
    val tidspunkt: LocalDate?,
    val varighet: KursvarighetDtoV2?
)

data class KursvarighetDtoV2(
    val varighet: Int?,
    val tidsenhet: KursVarighetEnhetDtoV2?
)

enum class KursVarighetEnhetDtoV2 {
    TIME,
    DAG,
    UKE,
    MND,
    UKJENT
}

data class ForerkortDtoV2(
    val klasse: String?,
)

data class AnnenErfaringDtoV2(
    val rolle: String?,
    val beskrivelse: String?,
    val fraDato: LocalDate?,
    val tilDato: LocalDate?
)

data class GodkjenningDtoV2(
    val tittel: String?,
    val utsteder: String?,
    val gjennomfortDato: LocalDate?,
    val utloperDato: LocalDate?
)

data class ArbeidserfaringDtoV2(
    val tittel: String?,
    val arbeidsgiver: String?,
    val sted: String?,
    val beskrivelse: String?,
    val fraDato: LocalDate?,
    val tilDato: LocalDate?
)

data class UtdanningDtoV2(
    val tittel: String?,
    val utdanningsnivaa: String?,
    val studiested: String?,
    val beskrivelse: String?,
    val fraDato: LocalDate?,
    val tilDato: LocalDate?
)

data class FagdokumentasjonDtoV2(
    val tittel: String?,
    val type: Fagdokument?
)

enum class Fagdokument {
    SVENNEBREV_FAGBREV, MESTERBREV, AUTORISASJON
}

data class SertifikatDtoV2(
    val tittel: String?,
    val utsteder: String?,
    val gjennomfortDato: LocalDate?,
    val utloperDato: LocalDate?
)

data class FOJobbprofilDtoV2(
    val sistEndret: ZonedDateTime?,
    val onsketYrke: List<JobbprofilYrkeDtoV2>?,
    val onsketArbeidssted: List<JobbprofilArbeidsstedDtoV2>?,
    val onsketAnsettelsesform: List<JobbprofilAnsettelsesformDtoV2>?,
    val onsketArbeidstidsordning: List<JobbprofilArbeidstidsordningDtoV2>?,
    val onsketArbeidsdagordning: List<JobbprofilArbeidsdagordningDtoV2>?,
    val onsketArbeidsskiftordning: List<JobbprofilArbeidsskiftordningDto>?,
    val heltidDeltid: JobbprofilHeltidDeltidDtoV2?,
    val kompetanse: List<KompetanseDtoV2>?,
    val oppstart: String?
)

data class JobbprofilYrkeDtoV2(
    val tittel: String
)

data class JobbprofilArbeidsstedDtoV2(
    val stedsnavn: String
)

data class JobbprofilAnsettelsesformDtoV2(
    val tittel: String?
)

data class JobbprofilArbeidstidsordningDtoV2(
    val tittel: String?
)

data class JobbprofilArbeidsdagordningDtoV2(
    val tittel: String?
)

data class JobbprofilArbeidsskiftordningDto(
    val tittel: String?
)

data class JobbprofilHeltidDeltidDtoV2(
    val heltid: Boolean,
    val deltid: Boolean
)

data class KompetanseDtoV2(
    val tittel: String
)