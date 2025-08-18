package no.nav.veilarbvedtaksstotte.client.person.dto

import java.time.LocalDateTime

data class VergeData(
    val vergemaalEllerFremtidsfullmakt: List<VergemaalEllerFremtidsfullmakt>
) {

    data class VergemaalEllerFremtidsfullmakt(
        val type: Vergetype?,
        val embete: String?,
        val vergeEllerFullmektig: VergeEllerFullmektig?,
        val folkeregistermetadata: Folkeregistermetadata?
    )

    data class VergeEllerFullmektig(
        val navn: VergeNavn?,
        val motpartsPersonident: String?,
    )

    data class VergeNavn(
        val fornavn: String?,
        val mellomnavn: String?,
        val etternavn: String?
    )

    data class Folkeregistermetadata(
        val ajourholdstidspunkt: LocalDateTime?,
        val gyldighetstidspunkt: LocalDateTime?,
        val opphoerstidspunkt: LocalDateTime?
    )
}

enum class Vergetype {
    ENSLIG_MINDREAARIG_ASYLSOEKER,
    ENSLIG_MINDREAARIG_FLYKTNING,
    FORVALTNING_UTENFOR_VERGEMAAL,
    STADFESTET_FREMTIDSFULLMAKT,
    MINDREAARIG,
    MIDLERTIDIG_FOR_MINDREAARIG,
    VOKSEN,
    MIDLERTIDIG_FOR_VOKSEN
}
