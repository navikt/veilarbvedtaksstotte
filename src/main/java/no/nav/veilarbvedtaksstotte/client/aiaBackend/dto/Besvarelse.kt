package no.nav.veilarbvedtaksstotte.client.aiaBackend.dto

data class Besvarelse(
    val utdanning: Utdanning? = null,
    val utdanningBestatt: UtdanningBestatt? = null,
    val utdanningGodkjent: UtdanningGodkjent? = null,
    val helseHinder: HelseHinder? = null,
    val andreForhold: AndreForhold? = null,
    val sisteStilling: SisteStilling? = null,
    var dinSituasjon: DinSituasjon? = null,
    val fremtidigSituasjon: FremtidigSituasjon? = null,
    val tilbakeIArbeid: TilbakeIArbeid? = null,
) {

    data class Utdanning(
        val verdi: String? = null,
        val gjelderFraDato: String? = null,
        val gjelderTilDato: String? = null,
        val endretAv: String? = null,
        val endretTidspunkt: String? = null
    )

    data class UtdanningBestatt(
        val verdi: String? = null,
        val gjelderFraDato: String? = null,
        val gjelderTilDato: String? = null,
        val endretAv: String? = null,
        val endretTidspunkt: String? = null,
    )

    data class UtdanningGodkjent(
        val verdi: String? = null,
        val gjelderFraDato: String? = null,
        val gjelderTilDato: String? = null,
        val endretAv: String? = null,
        val endretTidspunkt: String? = null
    )

    data class HelseHinder(
        val verdi: String? = null,
        val gjelderFraDato: String? = null,
        val gjelderTilDato: String? = null,
        val endretAv: String? = null,
        val endretTidspunkt: String? = null
    )

    data class AndreForhold(
        val verdi: String? = null,
        val gjelderFraDato: String? = null,
        val gjelderTilDato: String? = null,
        val endretAv: String? = null,
        val endretTidspunkt: String? = null
    )

    data class SisteStilling(
        val verdi: String? = null,
        val gjelderFraDato: String? = null,
        val gjelderTilDato: String? = null,
        val endretAv: String? = null,
        val endretTidspunkt: String? = null
    )

    data class DinSituasjon(
        val verdi: String? = null,
        val tilleggsData: TilleggsData? = null,
        val gjelderFraDato: String? = null,
        val gjelderTilDato: String? = null,
        val endretAv: String? = null,
        val endretTidspunkt: String? = null
    ) {
        data class TilleggsData(
            val forsteArbeidsdagDato: String? = null,
            val sisteArbeidsdagDato: String? = null,
            val oppsigelseDato: String? = null,
            val gjelderFraDato: String? = null,
            val permitteringsProsent: String? = null,
            val stillingsProsent: String? = null,
            val permitteringForlenget: String? = null,
            val harNyJobb: String? = null
        )
    }

    data class FremtidigSituasjon(
        val verdi: String? = null,
        val gjelderFraDato: String? = null,
        val gjelderTilDato: String? = null,
        val endretAv: String? = null,
        val endretTidspunkt: String? = null
    )

    data class TilbakeIArbeid(
        val verdi: String? = null,
        val gjelderFraDato: String? = null,
        val gjelderTilDato: String? = null,
        val endretAv: String? = null,
        val endretTidspunkt: String? = null
    )
}