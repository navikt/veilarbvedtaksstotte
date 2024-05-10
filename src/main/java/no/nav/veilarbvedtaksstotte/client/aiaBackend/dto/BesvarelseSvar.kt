package no.nav.veilarbvedtaksstotte.client.aiaBackend.dto

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