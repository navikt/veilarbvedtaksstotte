package no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde;

import java.time.LocalDateTime

data class OyeblikksbildeRegistreringDto(
    val data: RegistreringResponseDto? = null,
    val journalfort: Boolean
)

data class RegistreringResponseDto(
    var registrering: RegistreringsdataDto? = null,
    var type: BrukerRegistreringType? = null
)

enum class BrukerRegistreringType {
    ORDINAER, SYKMELDT;
}

data class RegistreringsdataDto(
    val opprettetDato: LocalDateTime?,
    var besvarelse: BesvarelseSvar? = null,
    var teksterForBesvarelse: List<TekstForSporsmal> = emptyList(),
    val sisteStilling: Stilling?,
    val profilering: Profilering? = null,
    var manueltRegistrertAv: Veileder? = null,

    var endretAv: String?,
    var endretTidspunkt: LocalDateTime?
) {
    data class TekstForSporsmal(
        val sporsmalId: String,
        val sporsmal: String,
        var svar: String,
    )

    data class Stilling(
        val label: String? = null,
        val konseptId: Long = 0,
        val styrk08: String? = null,
    )

    data class Profilering(
        var innsatsgruppe: ProfilertInnsatsgruppe? = null,
        var alder: Int?,
        var jobbetSammenhengendeSeksAvTolvSisteManeder: Boolean?,
    )

    enum class ProfilertInnsatsgruppe {
        STANDARD_INNSATS,
        SITUASJONSBESTEMT_INNSATS,
        BEHOV_FOR_ARBEIDSEVNEVURDERING
    }

    data class NavEnhet(val id: String, val navn: String)

    data class Veileder(var ident: String? = null, var enhet: NavEnhet? = null)
}

data class BesvarelseSvar(
    val utdanning: UtdanningSvar? = null,
    val utdanningBestatt: UtdanningBestattSvar? = null,
    val utdanningGodkjent: UtdanningGodkjentSvar? = null,
    val helseHinder: HelseHinderSvar? = null,
    val andreForhold: AndreForholdSvar? = null,
    val sisteStilling: SisteStillingSvar? = null,
    var dinSituasjon: DinSituasjonSvar? = null,
    val fremtidigSituasjon: FremtidigSituasjonSvar? = null,
    val tilbakeIArbeid: TilbakeIArbeidSvar? = null,
) {
    enum class UtdanningSvar {
        INGEN_UTDANNING,
        GRUNNSKOLE,
        VIDEREGAENDE_GRUNNUTDANNING,
        VIDEREGAENDE_FAGBREV_SVENNEBREV,
        HOYERE_UTDANNING_1_TIL_4,
        HOYERE_UTDANNING_5_ELLER_MER,
        INGEN_SVAR;
    }

    enum class UtdanningGodkjentSvar {
        JA, NEI, VET_IKKE, INGEN_SVAR
    }

    enum class UtdanningBestattSvar {
        JA, NEI, INGEN_SVAR
    }

    enum class TilbakeIArbeidSvar {
        JA_FULL_STILLING,
        JA_REDUSERT_STILLING,
        USIKKER,
        NEI
    }

    enum class SisteStillingSvar {
        HAR_HATT_JOBB, HAR_IKKE_HATT_JOBB, INGEN_SVAR
    }

    enum class HelseHinderSvar {
        JA, NEI, INGEN_SVAR
    }

    enum class FremtidigSituasjonSvar {
        SAMME_ARBEIDSGIVER,
        SAMME_ARBEIDSGIVER_NY_STILLING,
        NY_ARBEIDSGIVER,
        USIKKER,
        INGEN_PASSER;
    }

    enum class DinSituasjonSvar {
        MISTET_JOBBEN,
        ALDRI_HATT_JOBB,
        HAR_SAGT_OPP,
        VIL_BYTTE_JOBB,
        ER_PERMITTERT,
        USIKKER_JOBBSITUASJON,
        JOBB_OVER_2_AAR,
        VIL_FORTSETTE_I_JOBB,
        AKKURAT_FULLFORT_UTDANNING,
        DELTIDSJOBB_VIL_MER,
        OPPSIGELSE,
        ENDRET_PERMITTERINGSPROSENT,
        TILBAKE_TIL_JOBB,
        NY_JOBB,
        MIDLERTIDIG_JOBB,
        KONKURS,
        SAGT_OPP,
        UAVKLART,
        ANNET
    }

    enum class AndreForholdSvar {
        JA, NEI, INGEN_SVAR
    }
}