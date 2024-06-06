package no.nav.veilarbvedtaksstotte.client.arbeidssoekeregisteret

import java.time.ZonedDateTime
import java.util.*

data class OpplysningerOmArbeidssoekerMedProfilering(
    val opplysningerOmArbeidssoeker: OpplysningerOmArbeidssoekerResponse? = null,
    val profilering: ProfileringResponse ? = null
)

// Opplysninger om arbeidssøker typer
data class OpplysningerOmArbeidssoekerRequest(val identitetsnummer: String, val periodeId: UUID)

data class OpplysningerOmArbeidssoekerResponse(
    val opplysningerOmArbeidssoekerId: UUID,
    val periodeId: UUID,
    val sendtInnAv: MetadataResponse,
    val utdanning: UtdanningResponse?,
    val helse: HelseResponse?,
    val annet: AnnetResponse?,
    val jobbsituasjon: List<BeskrivelseMedDetaljerResponse>
)

data class BeskrivelseMedDetaljerResponse(
    val beskrivelse: JobbSituasjonBeskrivelseResponse,
    val detaljer: Map<String, String>
)

enum class JobbSituasjonBeskrivelseResponse {
    UKJENT_VERDI,
    UDEFINERT,
    HAR_SAGT_OPP,
    HAR_BLITT_SAGT_OPP,
    ER_PERMITTERT,
    ALDRI_HATT_JOBB,
    IKKE_VAERT_I_JOBB_SISTE_2_AAR,
    AKKURAT_FULLFORT_UTDANNING,
    VIL_BYTTE_JOBB,
    USIKKER_JOBBSITUASJON,
    MIDLERTIDIG_JOBB,
    DELTIDSJOBB_VIL_MER,
    NY_JOBB,
    KONKURS,
    ANNET
}

data class AnnetResponse(
    val andreForholdHindrerArbeid: JaNeiVetIkke?
)

data class HelseResponse(
    val helsetilstandHindrerArbeid: JaNeiVetIkke
)

data class UtdanningResponse(
    val nus: String,    // NUS = Standard for utdanningsgruppering (https://www.ssb.no/klass/klassifikasjoner/36/)
    val bestaatt: JaNeiVetIkke?,
    val godkjent: JaNeiVetIkke?
)


data class ProfileringResponse(
    val profileringId: UUID,
    val periodeId: UUID,
    val opplysningerOmArbeidssoekerId: UUID,
    val sendtInnAv: MetadataResponse,
    val profilertTil: ProfilertTil,
    val jobbetSammenhengendeSeksAvTolvSisteManeder: Boolean?,
    val alder: Int?
)

enum class ProfilertTil {
    UKJENT_VERDI,
    UDEFINERT,
    ANTATT_GODE_MULIGHETER,
    ANTATT_BEHOV_FOR_VEILEDNING,
    OPPGITT_HINDRINGER
}

// Felles typer
enum class JaNeiVetIkke {
    JA, NEI, VET_IKKE
}

data class MetadataResponse(
    val tidspunkt: ZonedDateTime,
    val utfoertAv: BrukerResponse,
    val kilde: String,
    val aarsak: String
)

data class BrukerResponse(
    val type: BrukerType,
    val id: String?
)

enum class BrukerType {
    UKJENT_VERDI, UDEFINERT, VEILEDER, SYSTEM, SLUTTBRUKER
}

